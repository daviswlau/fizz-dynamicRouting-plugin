package we.plugin.dynamicrouting.el;

import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.ParserContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author davis lau
 */
public class SpelTemplateContext implements TemplateContext {

    private static final String EXPRESSION_REGEX = "\\{([^#|T|(])";
    private static final Pattern EXPRESSION_REGEX_PATTERN = Pattern.compile(EXPRESSION_REGEX);
    private static final String EXPRESSION_REGEX_SUBSTITUTE = "{'{'}$1";

    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext();

    private final StandardEvaluationContext context = new StandardEvaluationContext();

    public SpelTemplateContext() {
        context.registerFunction("jsonPath", BeanUtils.resolveSignature("evaluate", JsonPathFunction.class));
    }

    @Override
    public void setVariable(String name, Object value) {
        context.setVariable(name, value);
    }

    @Override
    public Object lookupVariable(String name) {
        return context.lookupVariable(name);
    }

    public EvaluationContext getContext() {
        return context;
    }

    private Expression parseExpression(String expression) {
        return new SpelExpressionParser().parseExpression(
                EXPRESSION_REGEX_PATTERN.matcher(expression).replaceAll(EXPRESSION_REGEX_SUBSTITUTE), PARSER_CONTEXT);
    }

    private <T> T getValue(Expression expression, Class<T> clazz) {
        return expression.getValue(context, clazz);
    }

    @Override
    public <T> T getValue(String expression, Class<T> clazz) {
        return getValue(parseExpression(expression), clazz);
    }

    @Override
    public <T> T getSimpleValue(String expression, Class<T> clazz) {
        Expression simpleExpression  = new SpelExpressionParser().parseExpression(expression);
        return getValue(simpleExpression, clazz);
    }
}
