package uk.gov.hmcts.reform.data.ingestion.camel.route.beans;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class RouteProperties {

    String routeName;

    String childNames;

    String sql;

    String deleteSql;

    String truncateSql;

    String blobPath;

    String processor;

    String mapper;

    String binder;

    String fileName;

    String tableName;

    String deferredSql;
}
