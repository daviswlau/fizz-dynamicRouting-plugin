package we.plugin.dynamicrouting.el;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.spi.cache.CacheProvider;
import com.jayway.jsonpath.spi.cache.NOOPCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author davis lau
 */
public final class JsonPathFunction {

    private static final Configuration CONFIGURATION;

    static {
        Configuration configuration = Configuration.defaultConfiguration();
        CONFIGURATION = configuration.addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);
        CacheProvider.setCache(new NOOPCache());
    }

    private JsonPathFunction() {
    }

    public static <T> T evaluate(Object json, String jsonPath, Predicate... predicates) throws IOException {
        if (json instanceof String) {
            return JsonPath.using(CONFIGURATION).parse((String)json).read(jsonPath, predicates);
        }
        else if (json instanceof File) {
            return JsonPath.using(CONFIGURATION).parse((File)json).read(jsonPath, predicates);
        }
        else if (json instanceof InputStream) {
            return JsonPath.using(CONFIGURATION).parse((InputStream)json).read(jsonPath, predicates);
        }
        else {
            return JsonPath.using(CONFIGURATION).parse(json).read(jsonPath, predicates);
        }
    }
}
