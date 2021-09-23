package we.plugin.dynamicrouting.el;

/**
 * @author davis lau
 */
public interface TemplateContext {

    /**
     * Set a named variable within this evaluation context to a specified value.
     * @param name variable to set
     * @param value value to be placed in the variable
     */
    void setVariable(String name, Object value);

    /**
     * Look up a named variable within this evaluation context.
     * @param name variable to lookup
     * @return the value of the variable
     */
    Object lookupVariable(String name);

    /**
     * getValue of expression
     * @param <T>
     * @param expression
     * @param clazz
     * @return the value of clazz defined
     */
    <T> T getValue(String expression, Class<T> clazz);

    /**
     * getSimpleValue of expression
     * @param <T>
     * @param expression
     * @param clazz
     * @return the value of clazz defined
     */
    <T> T getSimpleValue(String expression, Class<T> clazz);
}
