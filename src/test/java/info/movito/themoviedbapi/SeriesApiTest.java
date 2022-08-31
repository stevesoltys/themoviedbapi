package info.movito.themoviedbapi;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import info.movito.themoviedbapi.model.config.Timezone;
import info.movito.themoviedbapi.model.core.TvKeywords;
import info.movito.themoviedbapi.model.providers.ProviderResults;
import info.movito.themoviedbapi.model.providers.WatchProviders;
import info.movito.themoviedbapi.model.tv.TvEpisode;
import info.movito.themoviedbapi.model.tv.TvSeason;
import info.movito.themoviedbapi.model.tv.TvSeries;
import info.movito.themoviedbapi.tools.ApiUrl;
import info.movito.themoviedbapi.tools.RequestMethod;
import info.movito.themoviedbapi.tools.UrlReader;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static info.movito.themoviedbapi.TmdbMovies.TMDB_METHOD_MOVIE;
import static info.movito.themoviedbapi.TmdbTV.TMDB_METHOD_TV;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class SeriesApiTest extends AbstractTmdbApiTest {

    public static final int BREAKING_BAD_SERIES_ID = 1396;

    @Test
    public void testGetWatchProviders() throws IOException {
        UrlReader mockUrlReader = mock(UrlReader.class);

        String configResponse = TestUtils.getFixture("fixtures/config_response.json");
        String mockResponse = TestUtils.getFixture("fixtures/watch_providers.json");

        ApiUrl configApiUrl = new ApiUrl("configuration");
        configApiUrl.addParam("api_key", getApiKey());

        doReturn(configResponse)
                .when(mockUrlReader)
                .request(
                        argThat(url -> url.getPath().equals(new ApiUrl("configuration").buildUrl().getPath())),
                        eq(null),
                        eq(RequestMethod.GET));

        TmdbApi testTmdb = new TmdbApi("dummy_api", mockUrlReader, true);

        int seriesId = 1234;
        ApiUrl expected = new ApiUrl(TMDB_METHOD_TV, seriesId, TmdbTV.TvMethod.watch_providers);
        doReturn(mockResponse)
                .when(mockUrlReader)
                .request(
                        argThat(url -> url.getPath().equals(expected.buildUrl().getPath())),
                        eq(null),
                        eq(RequestMethod.GET));

        ProviderResults results = testTmdb.getTvSeries().getWatchProviders(seriesId);

        assertTrue(true);
        assertEquals(41, results.getResults().size());

        WatchProviders gbProvider = results.getProvidersForCountry("GB");
        assertNotNull(gbProvider);
        assertEquals(8, gbProvider.getRentProviders().size());
        assertEquals(6, gbProvider.getBuyProviders().size());
        assertEquals(1, gbProvider.getFlatrateProviders().size());
        assertEquals("Virgin TV Go", gbProvider.getFlatrateProviders().get(0).getProviderName());
    }

    @Test
    public void getSeries() {
        TvSeries result = tmdb.getTvSeries().getSeries(BREAKING_BAD_SERIES_ID, LANGUAGE_ENGLISH,
                TmdbTV.TvMethod.credits, TmdbTV.TvMethod.external_ids, TmdbTV.TvMethod.watch_providers);

        assertNotNull("No results found", result);
        Assert.assertTrue("No results found", result.getNetworks().size() == 1);
        assertFalse(result.getWatchProviders().getResults().isEmpty());
    }

    @Test
    public void getSeriesGenres() {
        Integer MR_ROBOT_ID = 62560;
        TvSeries result = tmdb.getTvSeries().getSeries(MR_ROBOT_ID, LANGUAGE_ENGLISH);

        assertEquals("Unexpected genre count for mr robot", 2, result.getGenres().size());

//       TvResultsPage popular = tmdb.getTvSeries().getPopular(LANGUAGE_ENGLISH, 1);
//       System.out.println(popular);
    }

    @Test
    public void getSeriesKeywords() {
        Integer MR_ROBOT_ID = 62560;
        TvKeywords result = tmdb.getTvSeries().getKeywords(MR_ROBOT_ID, LANGUAGE_ENGLISH);

        assertEquals("Unexpected keyword count for mr robot", 8, result.getKeywords().size());

//       TvResultsPage popular = tmdb.getTvSeries().getPopular(LANGUAGE_ENGLISH, 1);
//       System.out.println(popular);
    }


    @Test
    public void getSeason() {
        TvSeason result = tmdb.getTvSeasons().getSeason(BREAKING_BAD_SERIES_ID, 5, LANGUAGE_ENGLISH);

        assertNotNull("No results found", result);
        Assert.assertTrue("episode number does not match", result.getEpisodes().get(0).getEpisodeNumber() == 1);
    }


    @Test
    public void getSeasonWithAppendedMethods() {
        TvSeason result = tmdb.getTvSeasons().getSeason(BREAKING_BAD_SERIES_ID, 5, LANGUAGE_ENGLISH, TmdbTvSeasons.SeasonMethod.values());

        assertNotNull("No results found", result);
        Assert.assertTrue("episode number does not match", result.getEpisodes().get(0).getEpisodeNumber() == 1);

        // todo add more asserts here that test the methods
    }


    @Test
    public void getEpisode() {
        TvEpisode episode = tmdb.getTvEpisodes().getEpisode(BREAKING_BAD_SERIES_ID, 5, 3, LANGUAGE_ENGLISH);

        assertNotNull("No results found", episode);
        Assert.assertTrue("episode number does not match", episode.getEpisodeNumber() == 3);
        Assert.assertEquals("episode titles does not match", "Hazard Pay", episode.getName());
    }


    @Test
    public void getEpisodeWithAppendedMethods() {
        TvEpisode episode = tmdb.getTvEpisodes().getEpisode(BREAKING_BAD_SERIES_ID, 5, 3, LANGUAGE_ENGLISH, TmdbTvEpisodes.EpisodeMethod.values());

        assertNotNull("No results found", episode);
        Assert.assertTrue("episode number does not match", episode.getEpisodeNumber() == 3);
        Assert.assertEquals("episode titles does not match", "Hazard Pay", episode.getName());
        Assert.assertEquals("episode titles does not match", 8, episode.getCredits().getCast().size());
        Assert.assertEquals("episode titles does not match", "4339518", episode.getExternalIds().getTvdbId());
    }


    @Test
    public void testHomelandEpisodeStills() {
        TvEpisode episode = tmdb.getTvEpisodes().getEpisode(1407, 1, 1, LANGUAGE_ENGLISH, TmdbTvEpisodes.EpisodeMethod.values());
        Assert.assertFalse(episode.getImages().getStills().isEmpty());

    }


    @Test
    public void testAiringToday() {
        // Try to find the first (of possibly many timezones) listed
        // for 'US'

        Timezone ca = Iterables.find(tmdb.getTimezones(), new Predicate<Timezone>() {
            @Override
            public boolean apply(Timezone input) {
                return input.getCountry().equals("US");
            }
        });
        TvResultsPage en = tmdb.getTvSeries().getAiringToday("en", null, ca);
        Assert.assertFalse(en.getResults().isEmpty());
    }


    @Test
    public void testGetMovieTrailers() {
        //todo implement me
//        List<Trailer> result = tmdb.getTvSeries().getSeries(46648, "", TmdbTV.TvMethod.values()).getTrailers();
//        assertFalse("Movie trailers missing", result.isEmpty());
    }

    @Test
    public void getContentRatings() {
        Integer MR_ROBOT_ID = 62560;
        TvSeries result = tmdb.getTvSeries().getSeries(MR_ROBOT_ID, LANGUAGE_ENGLISH, TmdbTV.TvMethod.content_ratings);

        assertEquals("Unexpected content ratings count for mr robot", 5, result.getContentRatings().size());
    }

}
