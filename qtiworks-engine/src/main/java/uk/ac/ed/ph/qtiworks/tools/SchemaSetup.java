/* Copyright (c) 2012-2013, University of Edinburgh.
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
package uk.ac.ed.ph.qtiworks.tools;

import uk.ac.ed.ph.qtiworks.config.BaseServicesConfiguration;
import uk.ac.ed.ph.qtiworks.config.JpaSetupConfiguration;
import uk.ac.ed.ph.qtiworks.config.ServicesConfiguration;
import uk.ac.ed.ph.qtiworks.tools.services.BootstrapServices;
import uk.ac.ed.ph.qtiworks.tools.services.SampleResourceImporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Entry point that sets up the DB schema, imports base data and then exits.
 * <p>
 * This will change as development proceeds.
 * <p>
 * <strong>DANGER:</strong> Do not run this on a production server as it WILL delete all of the
 * existing data!!!
 *
 * @author David McKain
 */
public final class SchemaSetup {

    private static final Logger logger = LoggerFactory.getLogger(SchemaSetup.class);

    public static void main(final String[] args) {
        final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        logger.info("Recreating the DB schema");
        ctx.register(JpaSetupConfiguration.class, BaseServicesConfiguration.class, ServicesConfiguration.class);
        ctx.refresh();

        logger.info("Added system default user and default public options");
        final BootstrapServices bootstrapServices = ctx.getBean(BootstrapServices.class);
        bootstrapServices.setupSystemDefaults();

        logger.info("Importing QTI samples");
        final SampleResourceImporter sampleResourceImporter = ctx.getBean(SampleResourceImporter.class);
        try {
            sampleResourceImporter.importQtiSamples();
        }
        finally {
            ctx.close();
        }
    }
}
