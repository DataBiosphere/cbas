version 1.0

task mock_task_1 {
  meta {
    description: "derived_cols mock task"
    volatile: true
  }

  input {
    File input_file_1
    String? optional_input_string_1
    Array[File] array_of_files_1 = []

    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }

  command {
    date '+%s' > mock_task_1_digest.txt
    echo 'Inputs:' >> mock_task_1_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_1_digest.txt
    echo 'optional_input_string_1: ~{optional_input_string_1}' >> mock_task_1_digest.txt
    echo 'array_of_files_1: ~{sep= ' ' array_of_files_1}' >> mock_task_1_digest.txt
  }

  runtime{
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_1_digest.txt"
  }
}

task mock_task_2 {
  meta {
    description: "nextstrain_ncov_defaults mock task"
    volatile: true
  }
   input {
     String input_string_1 = "30435fb9ec8de2f045167fb90adfec12f123e80a"
     String docker         = "nextstrain/base:build-20211012T204409Z"
   }

  command {
    date '+%s' > mock_task_2_digest.txt
    echo 'Inputs:' >> mock_task_2_digest.txt

    echo 'input_string_1: ~{input_string_1}' >> mock_task_2_digest.txt
    cp mock_task_2_digest.txt mock_task_2_digest_2.txt
    cp mock_task_2_digest.txt mock_task_2_digest_3.txt
    cp mock_task_2_digest.txt mock_task_2_digest_4.txt
    cp mock_task_2_digest.txt mock_task_2_digest_5.txt
    cp mock_task_2_digest.txt mock_task_2_digest_6.txt
    cp mock_task_2_digest.txt mock_task_2_digest_7.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_2_digest.txt"
    File output_file_2 = "mock_task_2_digest_2.txt"
    File output_file_3 = "mock_task_2_digest_3.txt"
    File output_file_4 = "mock_task_2_digest_4.txt"
    File output_file_5 = "mock_task_2_digest_5.txt"
    File output_file_6 = "mock_task_2_digest_6.txt"
    File output_file_7 = "mock_task_2_digest_7.txt"

  }
}
task mock_task_3 {
  meta {
    description: "zcat mock task"
    volatile: true
  }

  input {
    Array[File] input_array_of_files_1
    String input_string_1
    Int input_int_1 = 1
  }

  command {
    date '+%s' > mock_task_3_digest.txt
    echo 'Inputs:' >> mock_task_3_digest.txt

    echo 'input_array_of_files_1: ~{sep= ' ' input_array_of_files_1}' >> mock_task_3_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_3_digest.txt
    echo 'input_int_1: ~{input_int_1}' >> mock_task_3_digest.txt
  }

  runtime {
    docker: "quay.io/broadinstitute/viral-core:2.1.33"
  }

  output {
    File output_file_1 = "${input_string_1}"
    Int output_int_1 = 101
    Int output_int_2 = 201
    String output_string_1 = "mock task 3"
  }
}

task mock_task_4 {
  meta {
    description: "nextstrain_deduplicate_sequences mock task"
    volatile: true
  }

  input {
    File input_file_1
    Boolean input_bool = false
    String input_string_1 = "30435fb9ec8de2f045167fb90adfec12f123e80a"
    String docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_4_digest.txt
    echo 'Inputs:' >> mock_task_4_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_4_digest.txt
    echo 'input_bool: ~{input_bool}' >> mock_task_4_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_4_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_4_digest.txt"
  }
}

task mock_task_5 {
  meta {
    description: "filter_sequences_by_length mock task"
    volatile: true
  }

  input {
    File input_file_1
    Int input_int_1 = 1

    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }

  command {
    date '+%s' > mock_task_5_digest.txt
    echo 'Inputs:' >> mock_task_5_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_5_digest.txt
    echo 'input_int_1: ~{input_int_1}' >> mock_task_5_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_5_digest.txt"
    Int output_int_1 = 100
    Int output_int_2 = 200
    Int output_int_3 = 300
  }
}

task mock_task_6 {
  meta {
    description: "mafft_one_chr_chunked mock task"
    volatile: true
  }

  input {
    File input_file_1
    File? optional_input_file_1
    String input_string_1
    Boolean input_bool_1 = false

    Int input_int_1 = 1
    Int input_int_2 = 3
    String docker = "quay.io/broadinstitute/viral-phylo:2.1.19.1"
  }

  command {
    date '+%s' > mock_task_6_digest.txt
    echo 'Inputs:' >> mock_task_6_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_6_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_6_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_6_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_6_digest.txt
    echo 'input_int_1: ~{input_int_1}' >> mock_task_6_digest.txt
    echo 'input_int_2: ~{input_int_2}' >> mock_task_6_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_6_digest.txt"
    Int output_int_1 = 100
    Int output_int_2 = 200
    String output_string_1 = "output_string_1"
  }
}

task mock_task_7 {
  meta {
    description: "tsv_join mock task"
    volatile: true
  }

  input {
    Array[File]+   input_array_of_files
    String         input_string_1
    String         input_string_2 = "input_string_2"
    String         input_string_3 = "input_string_3"
    Boolean        input_bool_1 = true
    Int            input_int_1 = 7
  }

  command {
    date '+%s' > mock_task_7_digest.txt
    echo 'Inputs:' >> mock_task_7_digest.txt

    echo 'input_array_of_files: ~{sep=' ' input_array_of_files}' >> mock_task_7_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_7_digest.txt
    echo 'input_string_2: ~{input_string_2}' >> mock_task_7_digest.txt
    echo 'input_string_3: ~{input_string_3}' >> mock_task_7_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_7_digest.txt
    echo 'input_int_1: ~{input_int_1}' >> mock_task_7_digest.txt
  }

  runtime {
    docker: "quay.io/broadinstitute/viral-core:2.1.33"
  }

  output {
    File output_file_1 = "mock_task_7_digest.txt"
  }
}

task mock_task_8 {
  meta {
    description: "nextstrain_build_subsample mock task"
    volatile: true
  }

  input {
    File   input_file_1
    File   input_file_2
    String input_string_1
    File?  optional_input_file_1
    File?  optional_input_file_2
    File?  optional_input_file_3
    File?  optional_input_file_4

    Int?   optional_int_1
    String docker = "nextstrain/base:build-20211012T204409Z"
    String input_string_2 = "30435fb9ec8de2f045167fb90adfec12f123e80a"
  }

  command {
    date '+%s' > mock_task_8_digest.txt
    echo 'Inputs:' >> mock_task_8_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_8_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_8_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_8_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_8_digest.txt
    echo 'optional_input_file_2: ~{optional_input_file_2}' >> mock_task_8_digest.txt
    echo 'optional_input_file_3: ~{optional_input_file_3}' >> mock_task_8_digest.txt
    echo 'optional_input_file_4: ~{optional_input_file_4}' >> mock_task_8_digest.txt
    echo 'optional_int_1: ~{optional_int_1}' >> mock_task_8_digest.txt
    echo 'input_string_2: ~{input_string_2}' >> mock_task_8_digest.txt

    cp mock_task_8_digest.txt mock_task_8_digest_2.txt
    cp mock_task_8_digest.txt mock_task_8_digest_3.txt
    cp mock_task_8_digest.txt mock_task_8_digest_4.txt

  }

  runtime {
    docker: docker
  }

  output {
    File            output_file_1    = "mock_task_8_digest.txt"
    File            output_file_2    = "mock_task_8_digest_2.txt"
    File            output_file_3    = "mock_task_8_digest_3.txt"
    File            output_file_4    = "mock_task_8_digest_4.txt"
    Int             output_int_1     = 100
    Map[String,Int] map_string_int_1 = { "a": 1, "b": 2 }
    String          output_string_1  = "hello"
    Int             output_int_2     = 200
    Int             output_int_3     = 300
    String          output_string_2  = "world"
  }
}

task mock_task_9 {
  meta {
    description: "fasta_to_ids mock task"
    volatile: true
  }

  input {
    File input_file_1
  }

  command {
    date '+%s' > mock_task_9_digest.txt
    echo 'Inputs:' >> mock_task_9_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_9_digest.txt
  }

  runtime {
    docker: "ubuntu"
  }

  output {
    File output_file_1 = "mock_task_9_digest.txt"
  }
}

task mock_task_10 {
  meta {
    description: "snp_sites mock task"
    volatile: true
  }

  input {
    File    input_file_1
    Boolean input_bool_1 = true
    String  docker = "quay.io/biocontainers/snp-sites:2.5.1--hed695b0_0"
  }

  command {
    date '+%s' > mock_task_10_digest.txt
    echo 'Inputs:' >> mock_task_10_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_10_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_10_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_10_digest.txt"
    String output_string_1 = "mocking"
  }
}

task mock_task_11 {
  meta {
    description: "augur_mask_sites mock task"
    volatile: true
  }

  input {
    File   input_file_1
    File?  optional_input_file_1

    String docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_11_digest.txt
    echo 'Inputs:' >> mock_task_11_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_11_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_11_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1   = "mock_task_11_digest.txt"
    Int    output_int_1    = 1
    Int    output_int_2    = 2
    String output_string_1 = "hello cpu"
    String output_string_2 = "hello world"
  }
}

task mock_task_12 {
  meta {
    description: "draft_augur_tree mock task"
    volatile: true
  }

  input {
    File    input_file_1

    String  input_string_1 = "input_string_1"
    String  input_string_2 = "input_string_2"
    File?   optional_input_file_1
    File?   optional_input_file_2
    String? optional_string_1

    Int?    optional_int_1
    String  docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_12_digest.txt
    echo 'Inputs:' >> mock_task_12_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_12_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_12_digest.txt
    echo 'input_string_2: ~{input_string_2}' >> mock_task_12_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_12_digest.txt
    echo 'optional_input_file_2: ~{optional_input_file_2}' >> mock_task_12_digest.txt
    echo 'optional_string_1: ~{optional_string_1}' >> mock_task_12_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_12_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1     = "mock_task_12_digest.txt"
    Int output_int_1       = 100
    Int output_int_2       = 200
    String output_string_1 = "hellon world"
    String output_string_2 = "augur_version"
  }
}

task mock_task_13 {
  meta {
    description: "refine_augur_tree mock task"
    volatile: true
  }

  input {
    File     input_file_1
    File     input_file_2
    File     input_file_3

    Int?     optional_input_int_1
    Float?   optional_input_float_1
    Float?   optional_input_float_2
    Boolean  input_bool_1 = true
    String?  optional_input_string_1
    Boolean? optional_input_bool_1
    Boolean  input_bool_2 = false
    Int?     optional_input_int_2
    Boolean  input_bool_3 = true
    String?  optional_input_string_3 = "optional input string 3"
    String?  optional_input_string_4
    String?  optional_input_string_5
    Int?     optional_input_int_3 = 4
    String?  optional_input_string_6 = "optional input string 6"
    File?    optional_input_file_1

    String   docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_13_digest.txt
    echo 'Inputs:' >> mock_task_13_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_13_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_13_digest.txt
    echo 'input_file_3: ~{input_file_3}' >> mock_task_13_digest.txt

    echo 'optional_input_int_1: ~{optional_input_int_1}' >> mock_task_13_digest.txt
    echo 'optional_input_float_1: ~{optional_input_float_1}' >> mock_task_13_digest.txt
    echo 'optional_input_float_2: ~{optional_input_float_2}' >> mock_task_13_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_13_digest.txt


    echo 'optional_input_string_1: ~{optional_input_string_1}' >> mock_task_13_digest.txt
    echo 'optional_input_bool_1: ~{optional_input_bool_1}' >> mock_task_13_digest.txt
    echo 'input_bool_2: ~{input_bool_2}' >> mock_task_13_digest.txt
    echo 'optional_input_int_2: ~{optional_input_int_2}' >> mock_task_13_digest.txt
    echo 'input_bool_3: ~{input_bool_3}' >> mock_task_13_digest.txt
    echo 'optional_input_string_3: ~{optional_input_string_3}' >> mock_task_13_digest.txt
    echo 'optional_input_string_4: ~{optional_input_string_4}' >> mock_task_13_digest.txt
    echo 'optional_input_string_5: ~{optional_input_string_5}' >> mock_task_13_digest.txt

    echo 'optional_input_int_3: ~{optional_input_int_3}' >> mock_task_13_digest.txt
    echo 'optional_input_string_6: ~{optional_input_string_6}' >> mock_task_13_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_13_digest.txt
    cp mock_task_13_digest.txt mock_task_13_digest_2.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1   = "mock_task_13_digest.txt"
    File   output_file_2   = "mock_task_13_digest_2.txt"
    Int    output_int_1    = 10
    Int    output_int_2    = 15
    String output_string_1 = "output string 1"
    String output_string_2 = "output string 2"
  }
}

task mock_task_14 {
  meta {
    description: "ancestral_traits mock task"
    volatile: true
  }

  input {
    File          input_file_1
    File          input_file_2
    Array[String] input_array_of_strings_1

    Boolean       input_bool_1 = true
    File?         optional_input_file_1
    Float?        optional_input_float_1

    Int?          optional_input_int_1

    String        docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_14_digest.txt
    echo 'Inputs:' >> mock_task_14_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_14_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_14_digest.txt
    echo 'input_array_of_strings_1: ~{sep=' ' input_array_of_strings_1}' >> mock_task_14_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_14_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_14_digest.txt
    echo 'optional_input_float_1: ~{optional_input_float_1}' >> mock_task_14_digest.txt
    echo 'optional_input_int_1: ~{optional_input_int_1}' >> mock_task_14_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1    = "mock_task_14_digest.txt"
    Int    output_int_1     = 1
    Int    output_int_2     = 2
    String output_string_1  = "output string 1"
    String output_string_2  = "output string 2"
  }
}

task mock_task_15 {
  meta {
    description: "tip_frequencies mock task"
    volatile: true
  }

  input {
    File     input_file_1
    File     input_file_2

    String   input_string_2 = "input string 2"

    Float?   optional_input_float_1
    Float?   optional_input_float_2
    Int?     optional_input_int_1
    String?  optional_input_string_1
    Float?   optional_input_float_3
    Float?   optional_input_float_4
    Float?   optional_input_float_5
    Float?   optional_input_float_6
    Float?   optional_input_float_7
    Float?   optional_input_float_8
    Boolean  input_bool_1 = false
    Boolean  input_bool_2 = false

    Int?     optional_input_int_2
    String   docker = "nextstrain/base:build-20211012T204409Z"
    String   input_string_3 = "input string 3"
  }

  command {
    date '+%s' > mock_task_15_digest.txt
    echo 'Inputs:' >> mock_task_15_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_15_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_15_digest.txt
    echo 'input_string_2: ~{input_string_2}' >> mock_task_15_digest.txt
    echo 'optional_input_float_1: ~{optional_input_float_1}' >> mock_task_15_digest.txt
    echo 'optional_input_float_2: ~{optional_input_float_2}' >> mock_task_15_digest.txt
    echo 'optional_input_int_1: ~{optional_input_int_1}' >> mock_task_15_digest.txt
    echo 'optional_input_string_1: ~{optional_input_string_1}' >> mock_task_15_digest.txt
    echo 'optional_input_float_3: ~{optional_input_float_3}' >> mock_task_15_digest.txt
    echo 'optional_input_float_4: ~{optional_input_float_4}' >> mock_task_15_digest.txt
    echo 'optional_input_float_5: ~{optional_input_float_5}' >> mock_task_15_digest.txt
    echo 'optional_input_float_6: ~{optional_input_float_6}' >> mock_task_15_digest.txt
    echo 'optional_input_float_7: ~{optional_input_float_7}' >> mock_task_15_digest.txt
    echo 'optional_input_float_8: ~{optional_input_float_8}' >> mock_task_15_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_15_digest.txt
    echo 'input_bool_2: ~{input_bool_2}' >> mock_task_15_digest.txt
    echo 'optional_input_int_2: ~{optional_input_int_2}' >> mock_task_15_digest.txt
    echo 'input_string_3: ~{input_string_3}' >> mock_task_15_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1    = "mock_task_15_digest.txt"
    Int    output_int_1     = 1
    Int    output_int_2     = 2
    String output_string_1  = "output string 1"
    String output_string_2  = "output string 2"
  }
}

task mock_task_16 {
  meta {
    description: "ancestral_tree"
    volatile: true
  }

  input {
    File     input_file_1
    File     input_file_2

    String   input_string_1 = "input_string_1"
    Boolean  input_bool_1 = false
    Boolean  input_bool_2 = false
    Boolean  input_bool_3 = false
    File?    optional_input_file_1
    File?    optional_input_file_2

    String   docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_16_digest.txt
    echo 'Inputs:' >> mock_task_16_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_16_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_16_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_16_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_16_digest.txt
    echo 'input_bool_2: ~{input_bool_2}' >> mock_task_16_digest.txt
    echo 'input_bool_3: ~{input_bool_3}' >> mock_task_16_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_16_digest.txt
    echo 'optional_input_file_2: ~{optional_input_file_2}' >> mock_task_16_digest.txt
    cp mock_task_16_digest.txt mock_task_16_digest_2.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1   = "mock_task_16_digest.txt"
    File   output_file_2   = "mock_task_16_digest_2.txt"
    Int    output_int_1    = 1
    Int    output_int_2    = 2
    String output_string_1 = "an output string"
    String output_string_2 = "another output string"
  }
}

task mock_task_17 {
  meta {
    description: "translate_augur_tree mock task"
    volatile: true
  }

  input {
    File   input_file_1
    File   input_file_2
    File   input_file_3

    File?  optional_input_file_1
    File?  optional_input_file_2
    File?  optional_input_file_3

    String docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_17_digest.txt
    echo 'Inputs:' >> mock_task_17_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_17_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_17_digest.txt
    echo 'input_file_3: ~{input_file_3}' >> mock_task_17_digest.txt
    echo 'output_input_file_1: ~{optional_input_file_1}' >> mock_task_17_digest.txt
    echo 'output_input_file_2: ~{optional_input_file_2}' >> mock_task_17_digest.txt
    echo 'output_input_file_3: ~{optional_input_file_3}' >> mock_task_17_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1   = "mock_task_17_digest.txt"
    Int    output_int_1    = 1
    String output_string_1 = "an output string"
  }
}

task mock_task_18 {
  meta {
    description: "assign_clades_to_nodes mock task"
    volatile: true
  }

  input {
    File input_file_1
    File input_file_2
    File input_file_3
    File input_file_4
    File input_file_5

    String docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_18_digest.txt
    echo 'Inputs:' >> mock_task_18_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_18_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_18_digest.txt
    echo 'input_file_3: ~{input_file_3}' >> mock_task_18_digest.txt
    echo 'input_file_4: ~{input_file_4}' >> mock_task_18_digest.txt
    echo 'input_file_5: ~{input_file_5}' >> mock_task_18_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1    = "mock_task_18_digest.txt"
    Int    output_int_1     = 1
    String output_string_1  = "an output string"
  }
}

task mock_task_19 {
  meta {
    description: "export_auspice_json mock task"
    volatile: true
  }

  input {
    File           input_file_1
    File?          optional_input_file_1
    File           input_file_2
    Array[File]    input_array_of_files_1

    File?          optional_input_file_2
    File?          optional_input_file_3
    Array[String]? optional_input_array_of_strings1
    Array[String]? optional_input_array_of_strings2
    File?          optional_input_file_4
    Array[String]? optional_input_array_of_strings3
    String?        optional_input_string_1
    Boolean        input_bool_1 = true

    String input_string_1 = "an input string"

    Int?   optional_input_int_1
    String docker = "nextstrain/base:build-20211012T204409Z"
  }

  command {
    date '+%s' > mock_task_19_digest.txt
    echo 'Inputs:' >> mock_task_19_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_19_digest.txt
    echo 'optional_input_file_1: ~{optional_input_file_1}' >> mock_task_19_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_19_digest.txt
    echo 'input_array_of_files_1: ~{sep= ' ' input_array_of_files_1}' >> mock_task_19_digest.txt
    echo 'optional_input_file_2: ~{optional_input_file_2}' >> mock_task_19_digest.txt
    echo 'optional_input_file_3: ~{optional_input_file_3}' >> mock_task_19_digest.txt
    echo 'optional_input_array_of_strings_1: ~{sep= ' ' optional_input_array_of_strings1}' >> mock_task_19_digest.txt
    echo 'optional_input_array_of_strings_2: ~{sep= ' ' optional_input_array_of_strings2}' >> mock_task_19_digest.txt
    echo 'optional_input_file_4: ~{optional_input_file_4}' >> mock_task_19_digest.txt
    echo 'optional_input_array_of_strings_3: ~{sep= ' ' optional_input_array_of_strings3}' >> mock_task_19_digest.txt
    echo 'optional_input_string_1: ~{optional_input_string_1}' >> mock_task_19_digest.txt
    echo 'input_bool_1: ~{input_bool_1}' >> mock_task_19_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_19_digest.txt
    echo 'optional_input_int_1: ~{optional_input_int_1}' >> mock_task_19_digest.txt
    cp mock_task_19_digest.txt mock_task_19_digest_2.txt
  }

  runtime {
    docker: docker
  }

  output {
    File   output_file_1   = "mock_task_19_digest.txt"
    File   output_file_2   = "mock_task_19_digest_2.txt"
    Int    output_int_1    = 1
    Int    output_int_2    = 2
    String output_string_1 = "an output string"
    String output_string_2 = "again, an output string"
  }
}


workflow target_workflow_3 {
  meta {
    description: "Target workflow 3 for support in Q4 2022"
  }

  input {
      Array[File]+   array_of_files_input_1 = ["assembly_fastas array"]
      Array[File]+   array_of_files_input_2 = ["sample_metadata_tsvs"]
      File?          optional_input_file_1
      Int            input_int_1 = 27
      String         input_string_1 = "input string 1"

      String         input_string_2
      File           input_file_2

      Array[String]? array_of_strings_input_1

      File?          optional_input_file_2
      File?          optional_input_file_3
      File?          optional_input_file_4
  }

  call mock_task_2

  call mock_task_3 {
    input:
      input_array_of_files_1 = array_of_files_input_1,
      input_string_1 = "mock_task_3_digest.txt"
  }

  call mock_task_4 as call_1 {
    input:
      input_file_1 = mock_task_3.output_file_1
  }

  call mock_task_5 {
    input:
      input_file_1 = call_1.output_file_1,
      input_int_1 = input_int_1
  }

  call mock_task_6 as call_2 {
    input:
      input_file_1 = mock_task_5.output_file_1,
      optional_input_file_1 = select_first([optional_input_file_1, mock_task_2.output_file_3]),
      input_string_1 = "input_string_1"
  }

  if(length(array_of_files_input_2)>1) {
    call mock_task_7 {
      input:
        input_array_of_files = array_of_files_input_2,
        input_string_1 = "input_string_1",
        input_string_2 = "basename input",
        input_int_1 = 3
    }
  }

  call mock_task_1 {
    input:
      input_file_1 = select_first(flatten([[mock_task_7.output_file_1], array_of_files_input_2]))
  }

  call mock_task_8 as call_3 {
    input:
      input_file_1          = call_2.output_file_1,
      input_file_2          = mock_task_1.output_file_1,
      input_string_1        = input_string_2,
      optional_input_file_1 = input_file_2
  }

  call mock_task_9 {
    input:
      input_file_1 = call_3.output_file_1
  }

  call mock_task_10 {
    input:
      input_file_1 = call_3.output_file_1
  }

  call mock_task_11 {
    input:
      input_file_1 = call_3.output_file_1
  }

  call mock_task_12 {
    input:
      input_file_1 = mock_task_11.output_file_1
    }

  call mock_task_13 {
    input:
      input_file_1 = mock_task_12.output_file_1,
      input_file_2 = mock_task_11.output_file_1,
      input_file_3 = mock_task_1.output_file_1,
      optional_input_string_1 = input_string_1
  }

  if(defined(array_of_strings_input_1) && length(select_first([array_of_strings_input_1, []]))>0) {
    call mock_task_14 {
      input:
        input_file_1 = mock_task_13.output_file_1,
        input_file_2 = mock_task_1.output_file_1,
        input_array_of_strings_1 = select_first([array_of_strings_input_1, []])
    }
  }

  call mock_task_15 {
    input:
      input_file_1 = mock_task_13.output_file_1,
      input_file_2 = mock_task_1.output_file_1,
      optional_input_float_1 = 2020.0,
      optional_input_int_2 = 1,
      optional_input_string_1 = "units",
      optional_input_float_3 = 0.05,
      optional_input_float_5 = 0.0,
      input_string_3 = "out basename"
  }

  call mock_task_16 {
    input:
      input_file_1 = mock_task_13.output_file_1,
      input_file_2 = mock_task_11.output_file_1
  }

  call mock_task_17 {
    input:
      input_file_1 = mock_task_13.output_file_1,
      input_file_2 = mock_task_16.output_file_1,
      input_file_3 = mock_task_2.output_file_4
  }

  call mock_task_18 {
    input:
      input_file_1 = mock_task_13.output_file_1,
      input_file_2 = mock_task_16.output_file_1,
      input_file_3 = mock_task_17.output_file_1,
      input_file_4 = select_first([optional_input_file_1, mock_task_2.output_file_3]),
      input_file_5 = select_first([optional_input_file_3, mock_task_2.output_file_1])
  }

  call mock_task_19 {
    input:
      input_file_2 = mock_task_13.output_file_1,
      optional_input_file_1 = mock_task_1.output_file_1,
      optional_input_file_4 = select_first([optional_input_file_4, mock_task_2.output_file_2]),
      input_array_of_files_1 = select_all([
                                 mock_task_13.output_file_2,
                                 mock_task_14.output_file_1,
                                 mock_task_16.output_file_1,
                                 mock_task_17.output_file_1,
                                 mock_task_18.output_file_1]),
      optional_input_file_2 = select_first([optional_input_file_2, mock_task_2.output_file_7]),
      input_string_1 = "out basename"
  }

  output {
    File output_file_1                       = mock_task_3.output_file_1
    File output_file_2                       = call_2.output_file_1
    File output_file_3                       = mock_task_3.output_file_1

    File output_file_4                       = mock_task_1.output_file_1
    File output_file_5                       = mock_task_9.output_file_1
    File output_file_6                       = call_3.output_file_1
    File output_file_7                       = mock_task_11.output_file_1
    Int output_int_1                         = call_3.output_int_1
    Map[String, Int] output_map_string_int_1 = call_3.map_string_int_1

    File output_file_8                       = mock_task_12.output_file_1
    File output_file_9                       = mock_task_13.output_file_1
    Array[File] output_array_of_files_1      = select_all([
                                                  mock_task_13.output_file_2,
                                                  mock_task_14.output_file_1,
                                                  mock_task_16.output_file_1,
                                                  mock_task_17.output_file_1,
                                                  mock_task_18.output_file_1])
    File output_file_10                      = mock_task_15.output_file_1
    File output_file_11                      = mock_task_19.output_file_2
    File output_file_12                      = mock_task_19.output_file_1
  }
       }

