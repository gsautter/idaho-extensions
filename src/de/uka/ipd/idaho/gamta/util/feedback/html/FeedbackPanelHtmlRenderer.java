/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universitaet Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITAET KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.gamta.util.feedback.html;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.util.GamtaClassLoader;
import de.uka.ipd.idaho.gamta.util.GamtaClassLoader.ComponentInitializer;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AnnotationEditorFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.AssignmentDisambiguationFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.CategorizationFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.CheckBoxFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.StartContinueFeedbackPanelRenderer;
import de.uka.ipd.idaho.gamta.util.feedback.html.renderers.StartContinueOtherFeedbackPanelRenderer;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;

/**
 * Adapter for rendering a feedback panel as an HTML form, and for translating
 * the response back to the actual feedback panel. A renderer is actually a
 * factory for renderer instances, which wrap a given feedback panel. If no
 * factory is registered at all, the getRenderer() method returns default
 * renderers that loop all methods through to the wrapped feedback panel.
 * 
 * @author sautter
 */
public abstract class FeedbackPanelHtmlRenderer {
	
	/**
	 * Renderer instances are created by respective renderers for wrapping one
	 * specific feedback panel. This is in order to determin how well a given
	 * renderer class is suited for a given feedback panel before creating
	 * renderer instances.
	 * 
	 * @author sautter
	 */
	public static abstract class FeedbackPanelHtmlRendererInstance {
		
		/** the feedback panel to render */
		protected FeedbackPanel fp;
		
		/**
		 * Constructor
		 * @param fp the feedback panel to render
		 */
		protected FeedbackPanelHtmlRendererInstance(FeedbackPanel fp) {
			this.fp = fp;
		}
		
		/**
		 * Write the body of the JavaScript page initializer function. This method
		 * is needed for displaying feedback panels as HTML pages. This default
		 * implementation loops the method through to the wrapped feedback panel.
		 * Sub classes are welcome to overwrite it as needed.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writeJavaScriptInitFunctionBody(Writer out) throws IOException {
			this.fp.writeJavaScriptInitFunctionBody(out);
		}
		
		/**
		 * Write the body of the JavaScript pre-submission check function. This
		 * method is needed for checking the status of HTML feedback forms
		 * before submission. This default implementation loops the method
		 * through to the wrapped feedback panel. Sub classes are welcome to
		 * overwrite it as needed.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writeJavaScriptCheckFeedbackFunctionBody(Writer out) throws IOException {
			this.fp.writeJavaScriptCheckFeedbackFunctionBody(out);
		}
		
		/**
		 * Write the body of the JavaScript submission preparation function.
		 * This method allows for final pre-submit modifications to HTML
		 * feedback forms. This default implementation loops the method through
		 * to the wrapped feedback panel. Sub classes are welcome to overwrite
		 * it as needed.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writeJavaScriptPrepareSubmitFunctionBody(Writer out) throws IOException {
			this.fp.writeJavaScriptPrepareSubmitFunctionBody(out);
		}
		
		/**
		 * Write further JavaScript code (variables and functions). This method is
		 * needed for displaying feedback panels as HTML pages. This default
		 * implementation loops the method through to the wrapped feedback panel.
		 * Sub classes are welcome to overwrite it as needed.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writeJavaScript(Writer out) throws IOException {
			this.fp.writeJavaScript(out);
		}
		
		/**
		 * Write the CSS styles needed for the HTML representation of this feedback
		 * panel. This method is needed for displaying feedback panels as HTML
		 * pages. This default implementation loops the method through to the
		 * wrapped feedback panel. Sub classes are welcome to overwrite it as
		 * needed.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writeCssStyles(Writer out) throws IOException {
			this.fp.writeCssStyles(out);
		}
		
		/**
		 * Write the HTML representation of the feedback panel's content. Enclosing
		 * HTML based feedback engines are strongly recommended to enclose the
		 * output of this method in some sort of block, e.g. a p, div, or td. This
		 * method is needed for displaying feedback panels as HTML pages. This
		 * default implementation loops the method through to the wrapped feedback
		 * panel. Sub classes are welcome to overwrite it as needed.<br>
		 * <br>
		 * Sub classes overwriting this method are recommended (though not expected)
		 * to use a table for rendering the feedback panel. Instead of overwriting
		 * this method and provide a sub class specific implementation, it is also
		 * possible to use a specific feedback panel HTML renderer component. In
		 * order to avoid exceptions, HTML feedback engines should therefore first
		 * check if they have a renderer for a specific feedback panel, and only in
		 * case they don't have one use this method for rendering the page body.
		 * @param out the Writer to write to
		 * @throws IOException
		 */
		public void writePanelBody(Writer out) throws IOException {
			this.fp.writePanelBody(out);
		}
		
		/**
		 * Read in the response from a form rendered by this feedbacl panel's
		 * writePanelBody() method. This default implementation loops the method
		 * through to the wrapped feedback panel. Sub classes are welcome to
		 * overwrite it as needed.
		 * @param response a Properties object holding the response parameter/value
		 *            pairs
		 */
		public void readResponse(Properties response) {
			this.fp.readResponse(response);
		}
		
		/**
		 * Retrieve the overall complexity of the wrapped feedback panel. The
		 * complexity is intended to provide a measure for how long users may
		 * take to answer the feedback panel. It should be (somehow) related to
		 * the product of the decision complexity and the number of decisions,
		 * though need not be exactly this value. This method is useful, for
		 * instance, for determining the timeout of a dialog shown to a user
		 * remotely as an HTML page. This default implementation loops the
		 * method through to the wrapped feedback panel. Sub classes are welcome
		 * to overwrite it as needed.
		 * @return the complexity of the individual feedback decisions a user
		 *         has to make for answering the feedback panel.
		 */
		public int getComplexity() {
			return this.fp.getComplexity();
		}
		
		/**
		 * Retrieve the complexity of the wrapped feedback panel. The complexity
		 * is intended to provide a measure for how long users may take to
		 * decide on a single feedback parameter residing in the feedback panel.
		 * This method is useful, for instance, for determining the timeout of a
		 * dialog shown to a user remotely as an HTML page. At the same time,
		 * the complexity gives a hint on how many votes are required in the
		 * worst case for reaching a (non-trivial, i.e.,
		 * non-first-vote-takes-all) majority. This default implementation loops
		 * the method through to the wrapped feedback panel. Sub classes are
		 * welcome to overwrite it as needed.
		 * @return the complexity of the individual feedback decisions a user
		 *         has to make for answering the feedback panel.
		 */
		public int getDecisionComplexity() {
			return this.fp.getDecisionComplexity();
		}
		
		/**
		 * Retrieve the size of the wrapped feedback panel. The size is intended
		 * to provide a measure for the number of decisions users have to make
		 * for answering the feedback panel. This method is useful, for
		 * instance, for determining the timeout of a dialog shown to a user
		 * remotely as an HTML page. This default implementation loops the
		 * method through to the wrapped feedback panel. Sub classes are welcome
		 * to overwrite it as needed.
		 * @return the number of feedback decisions a user has to make for
		 *         answering the feedback panel.
		 */
		public int getDecisionCount() {
			return this.fp.getDecisionCount();
		}
	}
	
	private static HashSet rendererClassNames = new HashSet();
	private static LinkedList renderers = new LinkedList();
	
	//	add default feedback panel renderers
	static {
		addRenderer(new AnnotationEditorFeedbackPanelRenderer());
		addRenderer(new AssignmentDisambiguationFeedbackPanelRenderer());
		addRenderer(new CategorizationFeedbackPanelRenderer());
		addRenderer(new CheckBoxFeedbackPanelRenderer());
		addRenderer(new StartContinueFeedbackPanelRenderer());
		addRenderer(new StartContinueOtherFeedbackPanelRenderer());
	}
	
	/**
	 * Add a renderer so it can be used for producing renderer instances for
	 * feedback panels. Renderers added through this method take precedence over
	 * the default renderers if their canRender() method returns the same value.
	 * @param renderer the renderer to add
	 */
	public static void addRenderer(FeedbackPanelHtmlRenderer renderer) {
		if ((renderer != null) && rendererClassNames.add(renderer.getClass().getName()))
			renderers.addFirst(renderer);
	}
	
	/**
	 * Add all the renderers found in a specific folder. First, the
	 * loadRenderers() method is used for getting the renderers. Then, all
	 * rendererss will be added to the renderer registry via the addRenderer()
	 * method. Renderers added through this method take precedence over the
	 * default renderers if their canRender() method returns the same value.
	 * @param folder the folder to search for renderers
	 */
	public static void addRenderers(File folder) {
		FeedbackPanelHtmlRenderer[] renderers = loadRenderers(folder);
		for (int f = 0; f < renderers.length; f++)
			addRenderer(renderers[f]);
	}
	
	/**
	 * Seek through the jar files in a given folder and load all renderers
	 * found. Supposing a renderer class is found in 'MyRenderer.jar', then this
	 * method instantiates the factory, sets its data path to the
	 * 'MyRendererData' folder of the argument folder (will be created if not
	 * existing), and invokes the renderer instance's init() method. If the
	 * renderer requires any additional jars to be loaded, they have to reside
	 * in the argument folder, or in the 'MyRendererBin' sub folder of the
	 * argument folder.<br>
	 * <br>
	 * It is not required to use this method for loading renderers. Feedback
	 * engines who like to load the renderers themselves are welcome to do so.
	 * The only contract to follow is that the data path has to be set before
	 * invoking the init() method.
	 * @param folder the folder to search for renderers
	 * @return an array holding the renderers found in the specified folder
	 */
	public static FeedbackPanelHtmlRenderer[] loadRenderers(final File folder) {
		Object[] rendererObjects = GamtaClassLoader.loadComponents(
				folder,
				FeedbackPanelHtmlRenderer.class, 
				new ComponentInitializer() {
					public void initialize(Object component, String componentJarName) throws Exception {
						File dataPath = new File(folder, (componentJarName.substring(0, (componentJarName.length() - 4)) + "Data"));
						if (!dataPath.exists()) dataPath.mkdir();
						((FeedbackPanelHtmlRenderer) component).setDataPath(dataPath);
						((FeedbackPanelHtmlRenderer) component).init();
					}
				});
		FeedbackPanelHtmlRenderer[] renderers = new FeedbackPanelHtmlRenderer[rendererObjects.length];
		for (int r = 0; r < rendererObjects.length; r++)
			renderers[r] = ((FeedbackPanelHtmlRenderer) rendererObjects[r]);
		return renderers;
	}
	
	/**
	 * Test whether there is a renderer registered for a given feedback panel.
	 * @param fp the feedback panel to render
	 * @return true if there is a renderer registered whose canRender() method
	 *         returns a non-negative match value for the specified feedback
	 *         panel
	 */
	public static boolean hasRenderer(FeedbackPanel fp) {
		if (fp == null) return false;
		int bestMatch = Integer.MAX_VALUE;
		
		//	check registered renderers
		for (Iterator fit = renderers.iterator(); fit.hasNext();) {
			FeedbackPanelHtmlRenderer renderer = ((FeedbackPanelHtmlRenderer) fit.next());
			int match = renderer.canRender(fp);
			if ((match > -1) && (match < bestMatch))
				bestMatch = match;
			/*
			 * could simply return true here, but future extensions might
			 * require a more detailed return value than a boolean, namely an
			 * int.
			 */
		}
		
		/*
		 * no specialized renderer found, check if the feedback panel declares
		 * the rendering methods for itself and will therefore be renderable by
		 * the default renderer.
		 */
		if (bestMatch == Integer.MAX_VALUE) {
			boolean gotWritePanelBody = false;
			boolean gotReadResponse = false;
			
			//	climb up class hierarchy and check where the critical methods are declared
			Class fpClass = fp.getClass();
			while ((!gotWritePanelBody || !gotReadResponse) && (fpClass != null) && !fpClass.getName().equals(FeedbackPanel.class.getName())) {
				Method[] declaredMethods = fp.getClass().getDeclaredMethods();
				for (int m = 0; m < declaredMethods.length; m++) {
					if ("writePanelBody".equals(declaredMethods[m].getName())) {
						Class[] parameterClasses = declaredMethods[m].getParameterTypes();
						if ((parameterClasses.length == 1) && parameterClasses[0].getName().equals(Writer.class.getName()))
							gotWritePanelBody = true;
					}
					else if ("readResponse".equals(declaredMethods[m].getName())) {
						Class[] parameterClasses = declaredMethods[m].getParameterTypes();
						if ((parameterClasses.length == 1) && parameterClasses[0].getName().equals(Properties.class.getName()))
							gotReadResponse = true;
					}
				}
				fpClass = fpClass.getSuperclass();
			}
			
			//	feedback panel overrides both critical methods of FeedbackPanel ==> suitable for default renderer
			if (gotWritePanelBody && gotReadResponse)
				bestMatch = 0;
		}
		
		//	found something?
		return (bestMatch != Integer.MAX_VALUE);
	}
	
	/**
	 * Retrieve a renderer instance for a given feedback panel. If more than one
	 * renderer is present, this method uses the canRender() method of the
	 * renderer factory for determining which renderer is best suited for the
	 * specified feedback panel.
	 * @param fp the feedback panel to obtain a renderer for
	 * @return a renderer for the specified feedback panel, or null, if no
	 *         renderer can render the specified feedback panel, or if the
	 *         feedback panel is null itself
	 */
	public static FeedbackPanelHtmlRendererInstance getRenderer(FeedbackPanel fp) {
		if (fp == null) return null;
		
		//	search custom renderer
		int bestMatch = Integer.MAX_VALUE;
		FeedbackPanelHtmlRenderer bestMatchRenderer = null;
		for (Iterator fit = renderers.iterator(); fit.hasNext();) {
			FeedbackPanelHtmlRenderer renderer = ((FeedbackPanelHtmlRenderer) fit.next());
			int match = renderer.canRender(fp);
			if ((match > -1) && (match < bestMatch)) {
				bestMatch = match;
				bestMatchRenderer = renderer;
			}
		}
		if (bestMatchRenderer != null)
			return bestMatchRenderer.getRendererInstance(fp);
		
		/*
		 * no specialized renderer found, check if the feedback panel declares
		 * the rendering methods for itself and will therefore be renderable by
		 * the default renderer.
		 */
		boolean gotWritePanelBody = false;
		boolean gotReadResponse = false;
		
		//	climb up class hierarchy and check where the critical methods are declared
		Class fpClass = fp.getClass();
		while ((!gotWritePanelBody || !gotReadResponse) && (fpClass != null) && !fpClass.getName().equals(FeedbackPanel.class.getName())) {
			Method[] declaredMethods = fp.getClass().getDeclaredMethods();
			for (int m = 0; m < declaredMethods.length; m++) {
				if ("writePanelBody".equals(declaredMethods[m].getName())) {
					Class[] parameterClasses = declaredMethods[m].getParameterTypes();
					if ((parameterClasses.length == 1) && parameterClasses[0].getName().equals(Writer.class.getName()))
						gotWritePanelBody = true;
				}
				else if ("readResponse".equals(declaredMethods[m].getName())) {
					Class[] parameterClasses = declaredMethods[m].getParameterTypes();
					if ((parameterClasses.length == 1) && parameterClasses[0].getName().equals(Properties.class.getName()))
						gotReadResponse = true;
				}
			}
			fpClass = fpClass.getSuperclass();
		}
		
		//	feedback panel overrides both critical methods of FeedbackPanel ==> suitable for default renderer
		if (gotWritePanelBody && gotReadResponse)
			return new FeedbackPanelHtmlRendererInstance(fp) {};
		
		
		//	no renderer found
		return null;
	}
	
	/** the data path of the factory, i.e., the folder where its data is located */
	protected File dataPath;
	
	/**
	 * Make the renderer factory know the folder where its data is located
	 * @param dataPath the data path of the factory
	 */
	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}
	
	/**
	 * Initialize the factory. This method is invoked after the data path is
	 * set. This default implementation does nothing, sub classes are
	 * welcome to overwrite it as needed.
	 */
	public void init() {}
	
	/**
	 * Indicate whether or not this renderer can render a given feedback panel
	 * as HTML. a return value of -1 indicates that it cannot. Any non-negative
	 * integer indicates the inheritance distance from the class of the
	 * specified feedback panel to the feedback panel class this renderer can
	 * handle. A return value of 1, for instance, indicates that the renderer is
	 * suited to the immediate super class of the argument feedback panel, a
	 * return value of 0 indicates a perfect match. This mechanism is intended
	 * for finding the best suited renderer for any given feedback panel.
	 * @param fp the feedback panel to render
	 * @return an integer indication how well this renderer is suited to the
	 *         specified feedback panel
	 */
	public abstract int canRender(FeedbackPanel fp);
	
	/**
	 * Produce a renderer instance for a given feedback panel
	 * @param fp the feedback panel to obtain a renderer instance for
	 * @return a renderer instance for the specified feedback panel
	 */
	public abstract FeedbackPanelHtmlRendererInstance getRendererInstance(FeedbackPanel fp);
	
	/**
	 * Compute the inheritance depth from a super class to a sub class, i.e.,
	 * the number of extensions from superClass to subClass. This is helpful in
	 * the canRender() method.
	 * @param superClass the super class
	 * @param subClass the sub class
	 * @return the number of inheritance steps from superClass to subClass, or
	 *         -1, if subClass is not a sub class of superClass
	 */
	protected static int getInheritanceDepth(Class superClass, Class subClass) {
		if (superClass.isAssignableFrom(subClass)) {
			Class testClass = subClass;
			int inheritanceDepth = 0;
			while (!testClass.getName().equals(superClass.getName())) {
				inheritanceDepth++;
				testClass = testClass.getSuperclass();
			}
			return inheritanceDepth;
		}
		else return -1;
	}
	
	/**
	 * Prepate a String for displaying in an HTML page. Since text can already
	 * contain HTML markup, this method first checks if the argument String
	 * starts with '&lt;HTML&gt;'. If so, it cuts the leading and tailing tags
	 * and leaves the rest of the string unchanged. If not, it escapes all
	 * special characters.
	 * @param string the String to prepare
	 * @return the argument String prepared in a way that it shows the same
	 *         characters in an HTML page as it would show in a JLabel
	 */
	public static String prepareForHtml(String string) {
		if (string == null) return null;
		else if (
				string.startsWith("<") // quick pre-check
				&&
				(string.length() > 7) // prevent StringIndexOutOfBoundsException for subsequent checks
				&&
				string.substring(0, 6).equalsIgnoreCase("<HTML>") // avoid converting the whole string to upper case
				&&
				string.substring(string.length() - 7).equalsIgnoreCase("</HTML>") // avoid converting the whole string to upper case
			)
			
			//	String with markup, just strip HTML indicator tags
			return string.substring(6, (string.length() - 7));
		
		//	plain String, encode special characters
//		else return IoTools.prepareForHtml(string);
		else return IoTools.prepareForHtml(string, HTML_CHAR_MAPPING);
	}
	
	/**
	 * a mapping of characters to use in
	 * de.uka.ipd.idaho.htmlXmlUtil.IoTools.prepareForHtml() to cause characters
	 * having HTML entity representations not understood by browsers to be
	 * mapped to themselves
	 */
	private static final Properties HTML_CHAR_MAPPING = new Properties();
	static {
		HTML_CHAR_MAPPING.setProperty("À", "À");
		HTML_CHAR_MAPPING.setProperty("Á", "Á");
		HTML_CHAR_MAPPING.setProperty("Â", "Â");
		HTML_CHAR_MAPPING.setProperty("Ã", "Ã");
		HTML_CHAR_MAPPING.setProperty("Ä", "Ä");
		HTML_CHAR_MAPPING.setProperty("Å", "Å");
		HTML_CHAR_MAPPING.setProperty("Æ", "Æ");
		HTML_CHAR_MAPPING.setProperty("à", "à");
		HTML_CHAR_MAPPING.setProperty("á", "á");
		HTML_CHAR_MAPPING.setProperty("â", "â");
		HTML_CHAR_MAPPING.setProperty("ã", "ã");
		HTML_CHAR_MAPPING.setProperty("ä", "ä");
		HTML_CHAR_MAPPING.setProperty("å", "å");
		HTML_CHAR_MAPPING.setProperty("æ", "æ");
		
		HTML_CHAR_MAPPING.setProperty("Ç", "Ç");
		HTML_CHAR_MAPPING.setProperty("ç", "ç");
		
		HTML_CHAR_MAPPING.setProperty("È", "È");
		HTML_CHAR_MAPPING.setProperty("É", "É");
		HTML_CHAR_MAPPING.setProperty("Ê", "Ê");
		HTML_CHAR_MAPPING.setProperty("Ë", "Ë");
		HTML_CHAR_MAPPING.setProperty("è", "è");
		HTML_CHAR_MAPPING.setProperty("é", "é");
		HTML_CHAR_MAPPING.setProperty("ê", "ê");
		HTML_CHAR_MAPPING.setProperty("ë", "ë");
		
		HTML_CHAR_MAPPING.setProperty("Ì", "Ì");
		HTML_CHAR_MAPPING.setProperty("Í", "Í");
		HTML_CHAR_MAPPING.setProperty("Î", "Î");
		HTML_CHAR_MAPPING.setProperty("Ï", "Ï");
		HTML_CHAR_MAPPING.setProperty("ì", "ì");
		HTML_CHAR_MAPPING.setProperty("í", "í");
		HTML_CHAR_MAPPING.setProperty("î", "î");
		HTML_CHAR_MAPPING.setProperty("ï", "ï");
		
		HTML_CHAR_MAPPING.setProperty("Ñ", "Ñ");
		HTML_CHAR_MAPPING.setProperty("ñ", "ñ");
		
		HTML_CHAR_MAPPING.setProperty("Ò", "Ò");
		HTML_CHAR_MAPPING.setProperty("Ó", "Ó");
		HTML_CHAR_MAPPING.setProperty("Ô", "Ô");
		HTML_CHAR_MAPPING.setProperty("Õ", "Õ");
		HTML_CHAR_MAPPING.setProperty("Ö", "Ö");
		HTML_CHAR_MAPPING.setProperty("Œ", "Œ");
		HTML_CHAR_MAPPING.setProperty("Ø", "Ø");
		HTML_CHAR_MAPPING.setProperty("ò", "ò");
		HTML_CHAR_MAPPING.setProperty("ó", "ó");
		HTML_CHAR_MAPPING.setProperty("ô", "ô");
		HTML_CHAR_MAPPING.setProperty("õ", "õ");
		HTML_CHAR_MAPPING.setProperty("ö", "ö");
		HTML_CHAR_MAPPING.setProperty("œ", "œ");
		HTML_CHAR_MAPPING.setProperty("ø", "ø");
		
		HTML_CHAR_MAPPING.setProperty("Ù", "Ù");
		HTML_CHAR_MAPPING.setProperty("Ú", "Ú");
		HTML_CHAR_MAPPING.setProperty("Û", "Û");
		HTML_CHAR_MAPPING.setProperty("Ü", "Ü");
		HTML_CHAR_MAPPING.setProperty("ù", "ù");
		HTML_CHAR_MAPPING.setProperty("ú", "ú");
		HTML_CHAR_MAPPING.setProperty("û", "û");
		HTML_CHAR_MAPPING.setProperty("ü", "ü");
		
		HTML_CHAR_MAPPING.setProperty("Ý", "Ý");
		HTML_CHAR_MAPPING.setProperty("ý", "ý");
		HTML_CHAR_MAPPING.setProperty("ÿ", "ÿ");
		
		HTML_CHAR_MAPPING.setProperty("ß", "ß");
		
		HTML_CHAR_MAPPING.setProperty("€", "€");
		
		HTML_CHAR_MAPPING.setProperty("–", "-");
		HTML_CHAR_MAPPING.setProperty("—", "-");
		
		HTML_CHAR_MAPPING.setProperty("'", "'");
//		HTML_CHAR_MAPPING.setProperty("‘", "'");
//		HTML_CHAR_MAPPING.setProperty("’", "'");
//		HTML_CHAR_MAPPING.setProperty("‚", "'");
		
//		HTML_CHAR_MAPPING.setProperty("“", "&quot;");
//		HTML_CHAR_MAPPING.setProperty("”", "&quot;");
//		HTML_CHAR_MAPPING.setProperty("„", "&quot;");
//		HTML_CHAR_MAPPING.setProperty("‹", "&quot;");
//		HTML_CHAR_MAPPING.setProperty("›", "&quot;");
		
//		HTML_CHAR_MAPPING.setProperty("†", "†");
//		HTML_CHAR_MAPPING.setProperty("‡", "‡");
		
//		HTML_CHAR_MAPPING.setProperty("…", "…");
		
//		HTML_CHAR_MAPPING.setProperty("‰", "‰");
//		HTML_CHAR_MAPPING.setProperty("™", "™");
		HTML_CHAR_MAPPING.setProperty("=", "=");
	}
}
