package we.plugin.dynamicrouting;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import we.plugin.FizzPluginFilter;
import we.plugin.FizzPluginFilterChain;
import we.plugin.dynamicrouting.el.TemplateContext;
import we.util.WebUtils;
import we.spring.http.server.reactive.ext.FizzServerHttpRequestDecorator;
import we.util.NettyDataBufferUtils;
import we.proxy.Route;
import we.plugin.auth.ApiConfig;
import we.plugin.dynamicrouting.el.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.net.URLDecoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;

import reactor.core.publisher.Flux;

/**
 *  动态路由插件，路由使用ng规则，条件使用SpEL规则
 * @author davis lau
 */
@Component(DynamicRoutingPluginFilter.DYNAMIC_ROUTING_FILTER) // 必须，且为插件 id
public class DynamicRoutingPluginFilter implements FizzPluginFilter {

    private static final Logger log = LoggerFactory.getLogger(DynamicRoutingPluginFilter.class);

    public static final String DYNAMIC_ROUTING_FILTER = "dynamicRoutingPluginFilter";

    private final static Pattern GROUP_NAME_PATTERN = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
    private final static String GROUP_ATTRIBUTE = "group";
    private final static String GROUP_NAME_ATTRIBUTE = "groupName";

    public static final String PATTERN_KEY = "pattern";
    public static final String CONDITION_KEY = "condition";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String SERVICE_KEY = "service";
    public static final String PATH_KEY = "path";

    public static final String REQUEST_STRING = "request";
    public static final String COOKIES_STRING = "cookies";
    public static final String HEADER_STRING = "headers";

    public static final String HTTP_STRING = "http";
    public static final String HTTPS_STRING = "https";

    public static final String CONTENT_TYPE_STRING = "Content-Type";

    private static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, Map<String, Object> config) {
        try {

            TemplateContext tcontext = new SpelTemplateContext();

            String patternDef = (String) config.get(PATTERN_KEY);
            String condition = (String) config.get(CONDITION_KEY);
            String endpoint = (String) config.get(ENDPOINT_KEY);
            String service = (String) config.get(SERVICE_KEY);
            String path = (String) config.get(PATH_KEY);

            ServerHttpRequest req = exchange.getRequest();
            // ServerHttpResponse rep = exchange.getResponse();
            // String path = req.getURI().getPath();
            // String port = Integer.toString(req.getURI().getPort());
            // String contentType =
            // exchange.getRequest().getHeaders().getFirst(CONTENT_TYPE_STRING);
            // String method = req.getMethod().name();
            URI requestUri = req.getURI();

            String schema = requestUri.getScheme();
            String decodedSubPath = URLDecoder.decode(requestUri.getPath(), Charset.defaultCharset().name());
            String originalSubPath = requestUri.getPath();

            log.info("Dynamic routing for path {}", originalSubPath);

            /* dynamic routing pattern handle */
            Pattern pattern = Pattern.compile(patternDef);
            boolean isRedirect = pattern.matcher(decodedSubPath).matches();

            Matcher match = pattern.matcher(originalSubPath);
            match.matches();
            String[] groups = new String[match.groupCount()];
            for (int idx = 0; idx < match.groupCount(); idx++) {
                groups[idx] = match.group(idx + 1);
            }
            tcontext.setVariable(GROUP_ATTRIBUTE, groups);

            Set<String> extractedGroupNames = getNamedGroupCandidates(pattern.pattern());
            Map<String, String> groupNames = extractedGroupNames.stream()
                    .collect(Collectors.toMap(groupName -> groupName, match::group));
            tcontext.setVariable(GROUP_NAME_ATTRIBUTE, groupNames);

            /* spring spel handle , for request only */
            if (StringUtils.isNotBlank(condition)) {
                TemplateContext conditionContext = new SpelTemplateContext();

                // StandardEvaluationContext context = new StandardEvaluationContext();
                conditionContext.setVariable(REQUEST_STRING, req);
                conditionContext.setVariable(COOKIES_STRING, req.getCookies().toSingleValueMap());
                conditionContext.setVariable(HEADER_STRING, req.getHeaders().toSingleValueMap());

                // ExpressionParser parser = new SpelExpressionParser();
                // Expression expr = parser.parseExpression(condition);
                isRedirect = conditionContext.getSimpleValue(condition, boolean.class);
            }

            /* redirect url handle */
            if (isRedirect) {
                if ((!HTTP_STRING.equals(schema) && !HTTPS_STRING.equals(schema))) {
                    return WebUtils.responseErrorAndBindContext(exchange, DYNAMIC_ROUTING_FILTER,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                }

                FizzServerHttpRequestDecorator request = (FizzServerHttpRequestDecorator) exchange.getRequest();

                return request.getBody().defaultIfEmpty(NettyDataBufferUtils.EMPTY_DATA_BUFFER).single()
                        .flatMap(body -> {

                            String newPath = tcontext.getValue(path, String.class);
                            log.info("Dynamic routing to path {}", newPath);
                            Route route = WebUtils.getRoute(exchange);

                            /* 服务发现配置 */
                            if (StringUtils.isNotBlank(service)) {
                                route.type(ApiConfig.Type.SERVICE_DISCOVERY);
                                route.backendService(service);
                            }
                            /* 反向代理配置 */
                            if (StringUtils.isNotBlank(endpoint)) {
                                route.type(ApiConfig.Type.REVERSE_PROXY);
                                route.nextHttpHostPort(endpoint);
                            }

                            route.backendPath(newPath);

                            return FizzPluginFilterChain.next(exchange);
                        });
            }

            return FizzPluginFilterChain.next(exchange);

        } catch (Exception e) {
            log.error("Dynaamic Routing plugin Exception", e);
            return WebUtils.responseErrorAndBindContext(exchange, DYNAMIC_ROUTING_FILTER,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private Set<String> getNamedGroupCandidates(String regex) {
        Set<String> namedGroups = new TreeSet<>();
        Matcher m = GROUP_NAME_PATTERN.matcher(regex);
        while (m.find()) {
            namedGroups.add(m.group(1));
        }
        return namedGroups;
    }
}
