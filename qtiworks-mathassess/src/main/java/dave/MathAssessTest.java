/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package dave;

import uk.ac.ed.ph.jqtiplus.JqtiExtensionManager;
import uk.ac.ed.ph.jqtiplus.internal.util.DumpMode;
import uk.ac.ed.ph.jqtiplus.internal.util.ObjectDumper;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlObjectReader;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlReader;
import uk.ac.ed.ph.jqtiplus.resolution.AssessmentObjectManager;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.serialization.QtiSaxDocumentFirer;
import uk.ac.ed.ph.jqtiplus.serialization.SaxFiringOptions;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.ResponseData;
import uk.ac.ed.ph.jqtiplus.types.StringResponseData;
import uk.ac.ed.ph.jqtiplus.utils.QueryUtils;
import uk.ac.ed.ph.jqtiplus.validation.ItemValidationResult;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ClassPathResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.xslt.XsltSerializationOptions;
import uk.ac.ed.ph.jqtiplus.xmlutils.xslt.XsltStylesheetManager;

import uk.ac.ed.ph.snuggletex.utilities.SimpleStylesheetCache;

import org.qtitools.mathassess.MathAssessExtensionPackage;

import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

/**
 * Developer test of MathAssess extensions
 * FIXME: Document this type
 *
 * @author David McKain
 */
public class MathAssessTest {

    public static void main(final String[] args) throws Exception {
        final URI inputUri = URI.create("classpath:/MAD01-SRinCO-demo.xml");

        final MathAssessExtensionPackage mathAssessPackage = new MathAssessExtensionPackage(new SimpleStylesheetCache());
        final JqtiExtensionManager jqtiExtensionManager = new JqtiExtensionManager(mathAssessPackage);
        try {
            jqtiExtensionManager.init();

            System.out.println("Reading " + inputUri);
            final QtiXmlReader qtiXmlReader = new QtiXmlReader(jqtiExtensionManager);
            final QtiXmlObjectReader objectReader = qtiXmlReader.createQtiXmlObjectReader(new ClassPathResourceLocator());
            final AssessmentObjectManager objectManager = new AssessmentObjectManager(objectReader);

            final ItemValidationResult validationResult = objectManager.resolveAndValidateItem(inputUri);
            final ResolvedAssessmentItem resolvedAssessmentItem = validationResult.getResolvedAssessmentItem();
            System.out.println("Validation result: " + ObjectDumper.dumpObject(validationResult, DumpMode.DEEP));
            System.out.println("Extensions used: " + QueryUtils.findExtensionsUsed(jqtiExtensionManager, resolvedAssessmentItem));
            System.out.println("Foreign namespaces: " + QueryUtils.findForeignNamespaces(resolvedAssessmentItem.getItemLookup().extractAssumingSuccessful()));

            assert validationResult.isValid();

            final XsltSerializationOptions serializationOptions = new XsltSerializationOptions();
            serializationOptions.setIndenting(true);

            final XsltStylesheetManager stylesheetManager = new XsltStylesheetManager();
            final TransformerHandler serializerHandler = stylesheetManager.getSerializerHandler(serializationOptions);

            final StringWriter serializedXmlWriter = new StringWriter();
            serializerHandler.setResult(new StreamResult(serializedXmlWriter));

            final QtiSaxDocumentFirer saxEventFirer = new QtiSaxDocumentFirer(jqtiExtensionManager, serializerHandler, new SaxFiringOptions());
            saxEventFirer.fireSaxDocument(resolvedAssessmentItem.getItemLookup().extractAssumingSuccessful());

            System.out.println("\n\nSerailized XML:\n" + serializedXmlWriter.toString());

            final ItemSessionState itemSessionState = new ItemSessionState();
            final ItemSessionController itemController = new ItemSessionController(jqtiExtensionManager, resolvedAssessmentItem, itemSessionState);

            System.out.println("\n\nInitialising");
            itemController.initialize(null);
            System.out.println("\nTemplate Values: " + itemSessionState.getTemplateValues());
            System.out.println("\nResponse Values: " + itemSessionState.getResponseValues());
            System.out.println("\nOutcome Values: " + itemSessionState.getOutcomeValues());

            System.out.println("\n\nBinding Math responses");
            final Map<String, ResponseData> responses = new HashMap<String, ResponseData>();
            responses.put("RESPONSE", new StringResponseData("1+x"));
            final List<Identifier> badResponses = itemController.bindResponses(responses);
            final List<Identifier> invalidResponses = itemController.validateResponses();
            System.out.println("Bad responses: " + badResponses);
            System.out.println("Invalid response: " + invalidResponses);
            System.out.println("Response Values: " + itemSessionState.getResponseValues());

            System.out.println("\n\nStarting response processiong");
            itemController.processResponses();
            System.out.println("Response processing finished");
            System.out.println("Outcome Values: " + itemSessionState.getOutcomeValues());
        }
        finally {
            jqtiExtensionManager.destroy();
        }
    }
}
