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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import we.plugin.dynamicrouting.el.TemplateContext;
import we.plugin.dynamicrouting.el.SpelTemplateContext;


/**
 * 动态路由单元测试
 * @author Davis Lau
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicRoutingFilterTest {

    @Before
    public void init() throws URISyntaxException {

    }

    @Test
    public void test_validPattern() {
        String patternDef = "/proxy/dyroute/v1/(.*)";
        String path = "/proxy/dyroute/v1/goto/to1";

        TemplateContext tcontext = new SpelTemplateContext();

        Pattern pattern = Pattern.compile(patternDef);
        Matcher match = pattern.matcher(path);
        match.matches();
        String[] groups = new String[match.groupCount()];
        for (int idx = 0; idx < match.groupCount(); idx++) {
            groups[idx] = match.group(idx + 1);
        }
        tcontext.setVariable("group", groups);
        
        String newPath = tcontext.getValue("/cond/v1/{#group[0]}", String.class);

        assertEquals("/cond/v1/goto/to1", newPath);

    }

    @Test
    public void test_validCondition() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("release", "gray");
        TemplateContext conditionContext = new SpelTemplateContext();
        conditionContext.setVariable("headers", headers);
        boolean isRedirect = conditionContext.getSimpleValue("#headers['release'] == 'gray'", boolean.class);
        assertEquals(Boolean.TRUE, isRedirect);
    }
}
