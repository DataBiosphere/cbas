version 1.0

task foo {
  meta {
    description: "Target workflow 1 for support in Q4 2022"
  }
  input {
    File    input_file_1
    File?   input_file_2
    String  string_1
    String  string_2
    String? string_3
    String? string_4
    String? string_5
    String  string_6
    String? string_7

    String  docker = "ubuntu:latest"
  }
  parameter_meta {
    input_file_1: 'Required input file 1'
    input_file_2: 'Optional input file 2'

    string_1: 'Required string input 1'
    string_2: 'Required string input 2'
    string_3: 'Optional string input 3'
    string_4: 'Optional string input 4'
    string_5: 'Optional string input 5'
    string_6: 'Required string input 6'
    string_7: 'Optional string input 7'

    docker: 'Optional override for a custom docker image'
  }
  command {
    date '+%s' > digest.txt
    echo 'Input Digest:' >> digest.txt
    md5sum ~{input_file_1} ~{input_file_2} >> digest.txt
    echo 'Input strings:' >> digest.txt
    echo 'string_1: ~{string_1}' >> digest.txt
    echo 'string_2: ~{string_2}' >> digest.txt
    echo 'string_3: ~{string_3}' >> digest.txt
    echo 'string_4: ~{string_4}' >> digest.txt
    echo 'string_5: ~{string_5}' >> digest.txt
    echo 'string_6: ~{string_6}' >> digest.txt
    echo 'string_7: ~{string_7}' >> digest.txt
  }
  runtime {
    docker: docker
    cpu: 2
    memory: "3 GB"
    disks: "local-disk 10 LOCAL"
    maxRetries: 2
  }
  output {
    File file_output = "digest.txt"
  }
}

workflow target_workflow_1 {

  call foo

  output {
    File target_workflow_1_file_output = foo.file_output
  }
}
