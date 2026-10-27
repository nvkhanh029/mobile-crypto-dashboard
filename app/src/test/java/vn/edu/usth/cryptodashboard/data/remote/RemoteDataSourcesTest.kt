package vn.edu.usth.cryptodashboard.data.remote

import com.android.volley.ClientError
import com.android.volley.NetworkResponse
import java.util.concurrent.Executor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.edu.usth.cryptodashboard.data.fake.DiskFakeFileReader
import vn.edu.usth.cryptodashboard.data.mapper.ChartMapper
import vn.edu.usth.cryptodashboard.data.mapper.CoinDetailMapper
import vn.edu.usth.cryptodashboard.data.mapper.CoinMapper
import vn.edu.usth.cryptodashboard.data.source.DataSourceCallback
import vn.edu.usth.cryptodashboard.data.thread.ImmediateMainThreadPoster
import vn.edu.usth.cryptodashboard.domain.model.AppError
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Coin
import vn.edu.usth.cryptodashboard.domain.model.CoinDetail
import vn.edu.usth.cryptodashboard.domain.model.Currency

/**
 * The four remote data sources, fed with the captured responses of `app/src/main/assets/fake`
 * instead of the network: each one must ask for the right URL and give the same models as the
 * mappers do for that file (`docs/04-api-contract.md` §2).
 */
class RemoteDataSourcesTest {

    private val disk = DiskFakeFileReader()
    private val fetcher = FakeApiFetcher()
    private val loader = RemoteLoader(fetcher, Executor { it.run() }, ImmediateMainThreadPoster())

    // ---------------------------------------------------------------- market list

    @Test
    fun marketList_usd() {
        fetcher.answerWith(disk.read("fake/markets_usd.json"))
        val callback = Recorder<List<Coin>>()

        MarketListRemoteDataSource(loader).getTopCoins(Currency.USD, callback)

        assertEquals(listOf(CoinGeckoUrls.marketList(Currency.USD)), fetcher.urls)
        assertEquals(CoinMapper.parseList(disk.read("fake/markets_usd.json")), callback.data)
        assertEquals(100, callback.data!!.size)
    }

    @Test
    fun marketList_vnd() {
        fetcher.answerWith(disk.read("fake/markets_vnd.json"))
        val callback = Recorder<List<Coin>>()

        MarketListRemoteDataSource(loader).getTopCoins(Currency.VND, callback)

        assertEquals(listOf(CoinGeckoUrls.marketList(Currency.VND)), fetcher.urls)
        assertEquals(CoinMapper.parseList(disk.read("fake/markets_vnd.json")), callback.data)
    }

    @Test
    fun marketList_emptyArrayIsAnEmptyListNotAnError() {
        fetcher.answerWith(disk.read("fake/edge/markets_empty.json"))
        val callback = Recorder<List<Coin>>()

        MarketListRemoteDataSource(loader).getTopCoins(Currency.USD, callback)

        assertNull(callback.error)
        assertTrue(callback.data!!.isEmpty())
    }

    @Test
    fun marketList_invalidJsonIsInvalidData() {
        fetcher.answerWith(disk.read("fake/edge/markets_invalid.json"))
        val callback = Recorder<List<Coin>>()

        MarketListRemoteDataSource(loader).getTopCoins(Currency.USD, callback)

        assertEquals(AppError.InvalidData, callback.error)
        assertNull(callback.data)
    }

    // ---------------------------------------------------------------- coins by ID

    @Test
    fun coinsByIds_asksForTheIdsAndGivesTheCoinsOfTheAnswer() {
        fetcher.answerWith(disk.read("fake/markets_by_ids_usd.json"))
        val callback = Recorder<List<Coin>>()
        val ids = listOf("bitcoin", "ethereum", "akedo")

        CoinsByIdsRemoteDataSource(loader).getCoinsByIds(ids, Currency.USD, callback)

        assertEquals(listOf(CoinGeckoUrls.coinsByIds(ids, Currency.USD)), fetcher.urls)
        assertEquals(CoinMapper.parseList(disk.read("fake/markets_by_ids_usd.json")), callback.data)
        assertEquals(3, callback.data!!.size)
    }

    // ---------------------------------------------------------------- coin detail

    @Test
    fun coinDetail_requestHasNoCurrencyAndTheMapperPicksTheRequestedOne() {
        fetcher.answerWith(disk.read("fake/detail_bitcoin.json"))
        val usd = Recorder<CoinDetail>()
        val vnd = Recorder<CoinDetail>()

        CoinDetailRemoteDataSource(loader).getCoinDetail("bitcoin", Currency.USD, usd)
        CoinDetailRemoteDataSource(loader).getCoinDetail("bitcoin", Currency.VND, vnd)

        val url = CoinGeckoUrls.coinDetail("bitcoin")
        assertEquals(listOf(url, url), fetcher.urls)
        assertEquals(CoinDetailMapper.parse(disk.read("fake/detail_bitcoin.json"), Currency.USD), usd.data)
        assertEquals(CoinDetailMapper.parse(disk.read("fake/detail_bitcoin.json"), Currency.VND), vnd.data)
        assertNotEquals(usd.data!!.currentPrice, vnd.data!!.currentPrice)
    }

    @Test
    fun coinDetail_missingFieldsFileStillGivesADetail() {
        fetcher.answerWith(disk.read("fake/edge/detail_missing_fields.json"))
        val callback = Recorder<CoinDetail>()

        CoinDetailRemoteDataSource(loader).getCoinDetail("bitcoin", Currency.USD, callback)

        assertNull(callback.error)
        assertEquals(
            CoinDetailMapper.parse(disk.read("fake/edge/detail_missing_fields.json"), Currency.USD),
            callback.data,
        )
    }

    // ---------------------------------------------------------------- chart

    @Test
    fun chart_asksForTheDaysAndCurrency() {
        fetcher.answerWith(disk.read("fake/chart_bitcoin_7d_vnd.json"))
        val callback = Recorder<List<ChartPoint>>()

        ChartRemoteDataSource(loader).getChart("bitcoin", 7, Currency.VND, callback)

        assertEquals(listOf(CoinGeckoUrls.chart("bitcoin", 7, Currency.VND)), fetcher.urls)
        assertEquals(ChartMapper.parse(disk.read("fake/chart_bitcoin_7d_vnd.json")), callback.data)
        assertEquals(169, callback.data!!.size)
    }

    @Test
    fun chart_aShortAnswerIsNotAnError() {
        fetcher.answerWith(disk.read("fake/edge/chart_short_1d_usd.json"))
        val callback = Recorder<List<ChartPoint>>()

        ChartRemoteDataSource(loader).getChart("bitcoin", 1, Currency.USD, callback)

        assertNull(callback.error)
        assertTrue(callback.data!!.size < 2)
    }

    // ---------------------------------------------------------------- failures

    @Test
    fun aFailedRequest_isAnAppErrorInEveryDataSource() {
        fetcher.failWith(ClientError(NetworkResponse(429, ByteArray(0), false, 0L, null)))
        val market = Recorder<List<Coin>>()
        val byIds = Recorder<List<Coin>>()
        val detail = Recorder<CoinDetail>()
        val chart = Recorder<List<ChartPoint>>()

        MarketListRemoteDataSource(loader).getTopCoins(Currency.USD, market)
        CoinsByIdsRemoteDataSource(loader).getCoinsByIds(listOf("bitcoin"), Currency.USD, byIds)
        CoinDetailRemoteDataSource(loader).getCoinDetail("bitcoin", Currency.USD, detail)
        ChartRemoteDataSource(loader).getChart("bitcoin", 1, Currency.USD, chart)

        assertEquals(AppError.RateLimited, market.error)
        assertEquals(AppError.RateLimited, byIds.error)
        assertEquals(AppError.RateLimited, detail.error)
        assertEquals(AppError.RateLimited, chart.error)
    }

    // ---------------------------------------------------------------- helper

    private class Recorder<T> : DataSourceCallback<T> {
        var data: T? = null
        var error: AppError? = null
        var calls = 0

        override fun onSuccess(data: T) {
            this.data = data
            calls++
        }

        override fun onError(error: AppError) {
            this.error = error
            calls++
        }
    }
}
