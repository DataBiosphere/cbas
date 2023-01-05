version 1.0

task mock_task {

  input {
    String  input_string_1

    Int?    optional_int_1
    String  docker = "quay.io/broadinstitute/ncbi-tools:2.10.7.10"
  }

  meta {
    volitile: true
  }

  command {
    date '+%s' > mock_task_digest.txt
    echo 'Inputs:' >> mock_task_digest.txt

    echo 'input_string_1: ~{input_string_1}' >> mock_task_1_digest.txt
    echo 'optional_int_1: ~{optional_int_1}' >> mock_task_1_digest.txt
    cp mock_task_digest.txt mock_task_digest_2.txt
    cp mock_task_digest.txt mock_task_digest_3.txt
  }

  runtime {
    docker: docker
  }

  output {
    File    output_file_1    = "mock_task_digest.txt"
    Int     output_int_1     = 1
    String  output_string_1  = "output string 1"
    String  output_string_2  = "output string 2"
    String  output_string_3  = "output string 3"
    String  output_string_4  = "output string 4"
    String  output_string_5  = "output string 5"
    String  output_string_6  = "output string 6"
    String  output_string_7  = "output string 7"
    String  output_string_8  = "output string 8"
    String  output_string_9  = "output string 9"
    String  output_string_10 = "output string 10"
    String  output_string_11 = "output string 11"
    File    output_file_2    = "mock_task_digest_2.txt"
    File    output_file_3    = "mock_task_digest_3.txt"
  }
}

workflow target_workflow_4 {
  meta {
    description: "fetch_sra_to_bam workflow"
  }

  call mock_task

  output {
    File   output_file_1    = mock_task.output_file_1
    String output_string_1  = mock_task.output_string_1
    String output_string_2  = mock_task.output_string_2
    String output_string_3  = mock_task.output_string_3
    String output_string_4  = mock_task.output_string_4
    String output_string_5  = mock_task.output_string_5
    String output_string_6  = mock_task.output_string_7
    String output_string_7  = mock_task.output_string_8
    String output_string_8  = mock_task.output_string_9
    String output_string_9  = mock_task.output_string_10
    String output_string_10 = mock_task.output_string_11
    File   output_file_2    = mock_task.output_file_2
  }

}
