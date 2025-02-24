package com.redhat.naps.process.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.services.api.ProcessService;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Component;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RiskAssessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FhirProcessMgmt {

    private final static Logger log = LoggerFactory.getLogger(FhirProcessMgmt.class);
    private CorrelationKeyFactory correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();

    @Autowired
    private ProcessService processService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${sepsisdetection.deployment.id}")
    private String deploymentId;

    @Value("${sepsisdetection.process.id}")
    private String processId;

    public Long startProcess(Patient patientObj, List<Observation> oEvent, String sepsisResponse, RiskAssessment assessment) {
        /* NOTE:  
            FHIR data object uses id convention of:   <FHIR data type>/id
            Will use just the latter substring (after the "/") as the process instance correlation key
        */
        String idBase = patientObj.getIdBase();
        String cKey = idBase.substring(idBase.indexOf("/")+1);
        log.debug("doProcessMessage() correlationKey = "+cKey);
        CorrelationKey correlationKey = correlationKeyFactory.newCorrelationKey(cKey);
    
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("patient", patientObj);

        //TO-DO:  When persisting this list of Observations as part of process instance, upon retrieval of pInstanceVariables ..... the server thread is placed in an infinite loop of JSON processing
        //parameters.put("observationList", oEvent);

        parameters.put("sepsisResponse",sepsisResponse);
        parameters.put("riskAssessment",assessment);
    
    
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute((TransactionStatus s) -> {
            Long pi = processService.startProcess(deploymentId, processId, correlationKey, parameters);
            log.info("Started process for patient " + patientObj.toString() + ". ProcessInstanceId = " + pi+" : correlationKey = "+correlationKey.getName());
            return pi;
        });
        
    }
    
}
