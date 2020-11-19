package ca.uhn.fhir.mdm.provider;

/*-
 * #%L
 * HAPI FHIR - Enterprise Master Patient Index
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.mdm.api.IMdmControllerSvc;
import ca.uhn.fhir.mdm.api.IMdmExpungeSvc;
import ca.uhn.fhir.mdm.api.IMdmMatchFinderSvc;
import ca.uhn.fhir.mdm.api.IMdmSubmitSvc;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MdmProviderLoader {
	@Autowired
	private FhirContext myFhirContext;
	@Autowired
	private ResourceProviderFactory myResourceProviderFactory;
	@Autowired
	private IMdmMatchFinderSvc myMdmMatchFinderSvc;
	@Autowired
	private IMdmControllerSvc myMdmControllerSvc;
	@Autowired
	private IMdmExpungeSvc myMdmExpungeSvc;
	@Autowired
	private IMdmSubmitSvc myMdmSubmitSvc;

	public void loadProvider() {
		switch (myFhirContext.getVersion().getVersion()) {
			case DSTU3:
				myResourceProviderFactory.addSupplier(() -> new MdmProviderDstu3(myFhirContext, myMdmControllerSvc, myMdmMatchFinderSvc, myMdmExpungeSvc, myMdmSubmitSvc));
				break;
			case R4:
				myResourceProviderFactory.addSupplier(() -> new MdmProviderR4(myFhirContext, myMdmControllerSvc, myMdmMatchFinderSvc, myMdmExpungeSvc, myMdmSubmitSvc));
				break;
			default:
				throw new ConfigurationException("MDM not supported for FHIR version " + myFhirContext.getVersion().getVersion());
		}
	}
}
