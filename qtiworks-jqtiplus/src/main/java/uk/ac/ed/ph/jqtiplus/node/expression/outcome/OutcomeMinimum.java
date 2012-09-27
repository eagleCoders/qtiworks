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
package uk.ac.ed.ph.jqtiplus.node.expression.outcome;

import uk.ac.ed.ph.jqtiplus.node.expression.ExpressionParent;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.running.AssessmentItemRefAttemptController;
import uk.ac.ed.ph.jqtiplus.running.ProcessingContext;
import uk.ac.ed.ph.jqtiplus.running.TestProcessingContext;
import uk.ac.ed.ph.jqtiplus.state.AssessmentItemRefState;
import uk.ac.ed.ph.jqtiplus.value.FloatValue;
import uk.ac.ed.ph.jqtiplus.value.MultipleValue;
import uk.ac.ed.ph.jqtiplus.value.NullValue;
import uk.ac.ed.ph.jqtiplus.value.SingleValue;
import uk.ac.ed.ph.jqtiplus.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * This expression, which can only be used in outcomes processing, simultaneously looks up the normalMinimum value
 * of an outcome variable in A sub-set of the items referred to in A test. Only variables with single cardinality are
 * considered. Items with no declared minimum are ignored. The result has cardinality multiple and base-type float.
 *
 * @author Jiri Kajaba
 */
public final class OutcomeMinimum extends OutcomeMinMax {

    private static final long serialVersionUID = 6240420428843348630L;

    /** Name of this class in xml schema. */
    public static final String QTI_CLASS_NAME = "outcomeMinimum";

    public OutcomeMinimum(final ExpressionParent parent) {
        super(parent, QTI_CLASS_NAME);
    }

    @Override
    protected Value evaluateSelf(final ProcessingContext context, final Value[] childValues, final int depth) {
        final TestProcessingContext testContext = (TestProcessingContext) context;
        final List<AssessmentItemRefState> itemRefStates = testContext.lookupItemRefStates();

        final List<SingleValue> resultValues = new ArrayList<SingleValue>();
        for (final AssessmentItemRefState itemRefState : itemRefStates) {
            final AssessmentItemRefAttemptController itemRefController = testContext.getItemRefController(itemRefState);
            final OutcomeDeclaration outcomeDeclaration = itemRefController.getItemController().getItem().getOutcomeDeclaration(getOutcomeIdentifier());
            if (outcomeDeclaration != null && outcomeDeclaration.getCardinality().isSingle()) {
                if (!outcomeDeclaration.getBaseType().isNumeric() || outcomeDeclaration.getNormalMaximum() == null) {
                    return NullValue.INSTANCE;
                }

                final double minimum = outcomeDeclaration.getNormalMinimum().doubleValue();
                final double weight = itemRefController.getItemRef().lookupWeight(getWeightIdentifier());

                resultValues.add(new FloatValue(minimum * weight));
            }
        }

        return MultipleValue.createMultipleValue(resultValues);
    }
}
