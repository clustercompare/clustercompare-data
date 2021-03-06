/* Copyright 2005-2006 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.stripes.tag.layout;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;

import net.sourceforge.stripes.exception.StripesJspException;
import net.sourceforge.stripes.exception.StripesRuntimeException;
import net.sourceforge.stripes.util.Log;

/**
 * Renders a named layout, optionally overriding one or more components in the layout. Any
 * attributes provided to the class other than 'name' will be placed into page context during
 * the evaluation of the layout, making them available to other tags, and in EL.
 *
 * @author Tim Fennell, Ben Gunter
 * @since Stripes 1.1
 */
public class LayoutRenderTag extends LayoutTag implements DynamicAttributes {
    private static final Log log = Log.getInstance(LayoutRenderTag.class);

    private String name;
    private LayoutContext context;
    private boolean contextIsNew, silent;

    /** Gets the name of the layout to be used. */
    public String getName() { return name; }

    /** Sets the name of the layout to be used. */
    public void setName(String name) { this.name = name; }

    /** Look up an existing layout context or create a new one if none is found. */
    public LayoutContext getContext() {
        if (context == null) {
            LayoutContext context = LayoutContext.lookup(pageContext);
            boolean contextIsNew = false;

            if (context == null || !context.isComponentRenderPhase()
                    || context.isComponentRenderPhase() && isChildOfCurrentComponent()) {
                context = LayoutContext.push(this);
                contextIsNew = true;
            }

            this.context = context;
            this.contextIsNew = contextIsNew;
        }

        return context;
    }

    /** Returns true if this tag is a child of the current component tag. */
    public boolean isChildOfCurrentComponent() {
        try {
            LayoutTag parent = getLayoutParent();
            return parent instanceof LayoutComponentTag
                    && ((LayoutComponentTag) parent).isCurrentComponent();
        }
        catch (StripesJspException e) {
            // This exception would have been thrown before this tag ever executed
            throw new StripesRuntimeException("Something has happened that should never happen", e);
        }
    }

    /** True if this is the outermost layout tag, the one that initiated the render process. */
    public boolean isOuterLayoutTag() {
        return getLayoutParent() == null;
    }

    /** Used by the JSP container to provide the tag with dynamic attributes. */
    public void setDynamicAttribute(String uri, String localName, Object value) throws JspException {
        getContext().getParameters().put(localName, value);
    }

    /**
     * On the first pass (see {@link LayoutContext#isComponentRenderPhase()}):
     * <ul>
     * <li>Push the values of any dynamic attributes into page context attributes for the duration
     * of the tag.</li>
     * <li>Create a new context and places it in request scope.</li>
     * <li>Include the layout definition page named by the {@code name} attribute.</li>
     * </ul>
     * 
     * @return EVAL_BODY_INCLUDE in all cases
     */
    @Override
    public int doStartTag() throws JspException {
        LayoutContext context = getContext();
        silent = context.getOut().isSilent();

        if (contextIsNew) {
            log.debug("Start layout init in ", context.getRenderPage());
            pushPageContextAttributes(context.getParameters());
        }

        if (context.isComponentRenderPhase()) {
            log.debug("Start component render phase for ", context.getComponent(), " in ",
                    context.getRenderPage());
            exportComponentRenderers();
        }

        // Render tags never output their contents directly
        context.getOut().setSilent(true, pageContext);

        return EVAL_BODY_INCLUDE;
    }

    /**
     * After the first pass (see {@link LayoutContext#isComponentRenderPhase()}):
     * <ul>
     * <li>Ensure the layout rendered successfully by checking {@link LayoutContext#isRendered()}.</li>
     * <li>Remove the current layout context from request scope.</li>
     * <li>Restore previous page context attribute values.</li>
     * </ul>
     * 
     * @return EVAL_PAGE in all cases.
     */
    @Override
	public int doEndTag() throws JspException {
        try {
            LayoutContext context = getContext();
            if (contextIsNew) {
                // Substitution of the layout writer for the regular JSP writer does not work for
                // the initial render tag. Its body evaluation still uses the original JSP writer
                // for output. Clear the output buffer before executing the definition page.
                if (isOuterLayoutTag()) {
                    try {
                        context.getOut().clear();
                    }
                    catch (IOException e) {
                        log.debug("Could not clear output buffer: ", e.getMessage());
                    }
                }

                log.debug("End layout init in ", context.getRenderPage());

                try {
                    log.debug("Start layout exec in ", context.getDefinitionPage());
                    boolean silent = context.getOut().isSilent();
                    context.getOut().setSilent(true, pageContext);
                    context.doInclude(pageContext, getName());
                    context.getOut().setSilent(silent, pageContext);
                    log.debug("End layout exec in ", context.getDefinitionPage());
                }
                catch (Exception e) {
                    throw new StripesJspException(
                        "An exception was raised while invoking a layout. The layout used was " +
                        "'" + getName() + "'. The following information was supplied to the render " +
                        "tag: " + context.toString(), e);
                }

                // Check that the layout actually got rendered as some containers will
                // just quietly ignore includes of non-existent pages!
                if (!context.isRendered()) {
                    throw new StripesJspException(
                            "Attempt made to render a layout that does not exist. The layout name " +
                            "provided was '" + getName() + "'. Please check that a JSP/view exists at " +
                            "that location within your web application."
                        );
                }

                LayoutContext.pop(pageContext);
                popPageContextAttributes(); // remove any dynattrs from page scope
            }

            if (context.isComponentRenderPhase()) {
                log.debug("End component render phase for ", context.getComponent(), " in ",
                        context.getRenderPage());
                cleanUpComponentRenderers();
            }

            // Restore output's silent flag
            context.getOut().setSilent(silent, pageContext);

            // Skip the rest of the page if this is the outer-most render tag
            return isOuterLayoutTag() ? SKIP_PAGE : EVAL_PAGE;
        }
        finally {
            this.context = null;
            this.contextIsNew = false;
            this.silent = false;
        }
    }
}
