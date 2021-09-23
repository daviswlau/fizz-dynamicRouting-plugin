/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.plugin.dynamicrouting;

import java.util.Map;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.springframework.web.server.ServerWebExchange;

import we.plugin.dynamicRouting.DynamicRoutingFilter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.HttpHeaders;

/**
 * @author Davis Lau
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicRoutingFilterTest {

    @Mock
    private DynamicRoutingPluginFilter dynamicRoutingPluginFilter;
    
    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    private Map<String, Object> config = new HashMap<String, Object>();

    // @Mock
    private String fixedConfig = "{'key1':'val1'}";

    @Before
    public void init() throws URISyntaxException {
        config.put("pattern", "/test/p1");
        config.put("condition", "#header.release == 'gray'");
        config.put("endpoint", "http://127.0.0.1/");
        config.put("service", "dyroute");
        config.put("path", "/cond/v1");

        // request = new ServerHttpRequest();

        URI uri = new URI("http://www.baidu.com");
        HttpHeaders heders = new HttpHeaders();

        when(request.getURI()).thenReturn(uri);
        when(request.getHeaders()).thenReturn(heders);

        exchange = exchange.mutate().request(request).build();
    }

    @Test
    public void test_validPattern() {

        // when(dynamicRoutingFilter.getGlobalConfig()).thenReturn(new GlobalConfig());
        // when(dynamicRoutingFilter.getFixedConfigCache()).thenReturn(null);
        // when(exchange.getRequest()).thenReturn(request);

        dynamicRoutingPluginFilter.filter(exchange, config).block();
        // when(dynamicRoutingPluginFilter.filter(exchange, config)).thenReturn(Mono<Void> ).block();

        // verify(exchange).getRequest().getURI();

        // assertNotNull(Boolean.TRUE);
    }
}
