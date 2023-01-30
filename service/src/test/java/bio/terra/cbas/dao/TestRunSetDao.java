package bio.terra.cbas.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.cbas.models.CbasRunSetStatus;
import bio.terra.cbas.models.Method;
import bio.terra.cbas.models.MethodVersion;
import bio.terra.cbas.models.RunSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRunSetDao {

  @Autowired RunSetDao runSetDao;
  @Autowired MethodDao methodDao;
  @Autowired MethodVersionDao methodVersionDao;

  String inputDef =
      """
            [
              {
                "input_name": "fetch_sra_to_bam.Fetch_SRA_to_BAM.SRA_ID",
                "input_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "source": {
                  "type": "record_lookup",
                  "record_attribute": "sra_id"
                }
              },
              {
                "input_name": "fetch_sra_to_bam.Fetch_SRA_to_BAM.docker",
                "input_type": {
                  "type": "optional",
                  "optional_type": {
                    "type": "primitive",
                    "primitive_type": "String"
                  }
                },
                "source": {
                  "type": "none"
                }
              },
              {
                "input_name": "fetch_sra_to_bam.Fetch_SRA_to_BAM.machine_mem_gb",
                "input_type": {
                  "type": "optional",
                  "optional_type": {
                    "type": "primitive",
                    "primitive_type": "Int"
                  }
                },
                "source": {
                  "type": "none"
                }
              }
            ]
            """;

  String outputDef =
      """
            [
              {
                "output_name": "fetch_sra_to_bam.sra_metadata",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "File"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sra_metadata"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.reads_ubam",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "File"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "reads_ubam"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.biosample_accession",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "biosample_accession"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sample_geo_loc",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sample_geo_loc"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sample_collection_date",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sample_collection_date"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sequencing_center",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sequencing_center"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sequencing_platform",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sequencing_platform"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.library_id",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "library_id"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.run_date",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "run_date"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sample_collected_by",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sample_collected_by"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sample_strain",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sample_strain"
                }
              },
              {
                "output_name": "fetch_sra_to_bam.sequencing_platform_model",
                "output_type": {
                  "type": "primitive",
                  "primitive_type": "String"
                },
                "destination": {
                  "type": "record_update",
                  "record_attribute": "sequencing_platform_model"
                }
              }
            ]
            """;

  @Test
  void retrievesSingleRunSet() {

    Method method =
        new Method(
            UUID.fromString("00000000-0000-0000-0000-000000000008"),
            "fetch_sra_to_bam",
            "fetch_sra_to_bam",
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            null,
            "Github");

    MethodVersion methodVersion =
        new MethodVersion(
            UUID.fromString("80000000-0000-0000-0000-000000000008"),
            method,
            "1.0",
            "fetch_sra_to_bam sample submission",
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            null,
            "https://raw.githubusercontent.com/broadinstitute/viral-pipelines/master/pipes/WDL/workflows/fetch_sra_to_bam.wdl");

    RunSet runSet =
        new RunSet(
            UUID.fromString("10000000-0000-0000-0000-000000000008"),
            methodVersion,
            "fetch_sra_to_bam workflow",
            "fetch_sra_to_bam sample submission",
            true,
            CbasRunSetStatus.COMPLETE,
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            OffsetDateTime.parse("2023-01-27T19:21:24.563932Z"),
            0,
            0,
            inputDef, // Compensating for the additional newline the db stores
            outputDef,
            "sample");

    RunSet actual = runSetDao.getRunSet(UUID.fromString("10000000-0000-0000-0000-000000000008"));

    assertEquals(runSet, actual);
  }

  @Test
  void retrievesAllRunSets() {

    List<RunSet> runSets = runSetDao.getRunSets(10);
    assertEquals(3, runSets.size());
  }
}
