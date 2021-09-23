package we.plugin.dynamicrouting.el;

import org.springframework.expression.ParserContext;

/**
 * @author davis lau
 */
public class TemplateParserContext implements ParserContext {

    @Override
    public String getExpressionPrefix() {
        return "{";
    }

    @Override
    public String getExpressionSuffix() {
        return "}";
    }

    @Override
    public boolean isTemplate() {
        return true;
    }
}
