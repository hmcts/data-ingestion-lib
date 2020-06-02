package uk.gov.hmcts.reform.ingestion.camel.mapper;

import java.util.Map;

public interface IMapper {

    Map<String, Object> getMap(Object userProfile);
}
