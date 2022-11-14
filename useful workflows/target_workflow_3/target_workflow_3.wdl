version 1.0

# From "../tasks/tasks_nextstrain.wdl" as nextstrain
task mock_task_1 {
  meta {
    description: "derived_cols mock task"
  }

  input {
    File input_file_1
    String? optional_input_string_1
    Array[File] array_of_files_1 = []

    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }
  Int int_1 = 10

  parameter_meta {
    optional_input_string_1: {
      description: "optional_input_string_1 description"
    }
    array_of_files_1: {
      description: " array_of_files_1 description"
    }
  }
  String string_1 = basename(basename(input_file_1, ".txt"), ".tsv")

  command {
    date '+%s' > mock_task_1_digest.txt
    echo 'Inputs:' >> mock_task_1_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_1_digest.txt
    echo 'optional_input_string_1: ~{optional_input_string_1}' >> mock_task_1_digest.txt
    echo 'array_of_files_1: ~{array_of_files_1}' >> mock_task_1_digest.txt
  }

  runtime{
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_1_digest.txt"
  }
}

task mock_task_2 {
  meta{
    description: "nextstrain_ncov_defaults mock task"
  }
   input {
     String input_string_1 = "30435fb9ec8de2f045167fb90adfec12f123e80a"
     String docker = "nextstrain/base:build-20211012T204409Z"
   }

  command {
    date '+%s' > mock_task_2_digest.txt
    echo 'Inputs:' >> mock_task_2_digest.txt

    echo 'input_string_1: ~{input_string_1}' >> mock_task_2_digest.txt
  }

  runtime {
    docker: docker
  }

  output {
    File output_file_1 = "mock_task_3_digest.txt"
    File output_file_2 = "mock_task_3_digest.txt"
    File output_file_3 = "mock_task_3_digest.txt"
    File output_file_4 = "mock_task_3_digest.txt"
    File output_file_5 = "mock_task_3_digest.txt"
    File output_file_6 = "mock_task_3_digest.txt"
    File output_file_7 = "mock_task_3_digest.txt"

  }
}
task mock_task_3 {
  meta {
    description: "zcat mock task"
  }

  input {
    Array[File] input_array_of_files_1
    String input_string_1
    Int input_int_1 = 2
  }

  command {
    date '+%s' > mock_task_3_digest.txt
    echo 'Inputs:' >> mock_task_3_digest.txt

    echo 'input_array_of_files_1: ~{input_array_of_files_1}' >> mock_task_3_digest.txt
    echo 'input_string_1: ~{input_string_1}' >> mock_task_3_digest.txt
    echo 'input_int_1: ~{input_int_1}' >> mock_task_3_digest.txt
  }

  runtime {
    docker: "quay.io/broadinstitute/viral-core:2.1.33"
  }

  output {
    File ouput_file_1 = "mock_task_3_digest.txt"
    Int output_int_1 = 101
    Int output_int_2 = 201
    String output_string_1 = "mock task 3"
  }
}

task mock_task_4 {
  meta {
    description: "nextstrain_deduplicate_sequences mock task"
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
    File output_file_1 = "mock_task_3_digest.txt"
  }
}

task mock_task_5 {
  meta {
    description: "filter_sequences_by_length mock task"
  }

  input {
    File input_file_1
    Int input_int_1 = 5

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
    File ouput_file_1 = "mock_task_5_digest.txt"
    Int ouput_int_1 = 100
    Int output_int_2 = 200
    Int output_int_3 = 300
  }
}

task mock_task_6 {
  meta {
    description: "mafft_one_chr_chunked mock task"
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
    Int ouput_int_1 = 100
    Int output_int_2 = 200
    String output_string_1 = "output_string_1"
  }
}

task mock_task_7 {
  meta {
    description: "tsv_join mock task"
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

    echo 'input_array_of_files: ~{input_array_of_files}' >> mock_task_7_digest.txt
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
  }

  runtime {
    docker: docker
  }

  output {
    File            output_file_1  = "mock_task_7_digest.txt"
    File            output_file_2 = "mock_task_7_digest.txt"
    File            output_file_3  = "mock_task_7_digest.txt"
    File            output_file_4  = "mock_task_7_digest.txt"
    Int             output_int_1   = 100
    Map[String,Int] map_string_int_1 = read_map("map_string_int_1")
    String          ouput_string_1   = "hello"
    Int             output_int_2      = 200
    Int             output_int_3     = 300
    String          ouput_string_2        = "world"
  }
}

task mock_task_9 {
  meta {
    description: "fasta_to_ids mock task"
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
    File   output_file_1 = "mock_task_11_digest.txt"
    Int    output_int_1       = 1
    Int    output_int_2      = 2
    String ouput_string_1         = "hello cpu"
    String ouput_string_2    = "hello world"
  }
}

task mock_task_12 {
  meta {
    description: "draft_augur_tree mock task"
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
}








workflow target_workflow_3 {
  meta {
    description: "Target workflow 3 for support in Q4 2022"
  }

  input {
      Array[File]+ array_of_files_input_1
      Array[File]+ array_of_files_input_2
      File? optional_input_file_1
      Int input_int_1
      String input_string_1

      String input_string_2
      File input_file_2

      Array[String]? array_of_strings_input_1

      File? optional_input_file_1
      File? optional_input_file_2
      File? optional_input_file_3
  }

  parameter_meta {
      array_of_files_input_1: 'Required array of files input 1'
      array_of_files_input_2: 'Required array of files input 2'
      optional_input_file_1: 'Optional input file 1'
      input_int_1: 'Input int 1'
      input_string_1: 'Input string 1'

      # From here below, are not defined in the original parameter_meta
      input_string_2: 'Input string 2' # build_name
      input_file_2: 'Input file 2' # builds_yaml

      array_of_strings_input_1: 'Array of strings input 1'

      optional_input_file_1: 'Optional input file 1'
      optional_input_file_2: 'Optional input file 2'
      optional_input_file_3: 'Optional input file 3'

  }

  call  foo.foo_defaults

  call foo_call_utils.mock {
    input:
      foo_files = array_of_files_input_1,
      output_file_1 = 'foo_files_output_1.foo'
  }

  call foo.doo_duplicates as mock_1 {
    input:
      foo_sequences_fasta = mock.mock_combined
  }

  call foo_call_utils.length_mock {
    input:
      foo_sequences_fasta = mock_1.mock_sequences,
      foo_min_non = input_int_1
  }

  call foo.chunked as mock_chunked {
    input:
      foo_sequences = length_mock.foo_filtered_fasta,
      optional_input_file_1 = select_first([optional_input_file_1, foo_defaults.foo_ref_fasta]),
      variable_1 = 'variable_one.fasta'
  }

  if(length(array_of_files_input_2)>1) {
    call foo_call_1.foo_join {
      input:
        variable_2 = array_of_files_input_2,
        foo_id = 'foo_id',
        foo_out = 'foo out name',
        foo_mem = 30
    }
  }

  call foo.cols {
    input:
      foo_metadata = select_first(flatten([[foo_join.foo_out], array_of_files_input_2]))
  }

  call foo.foo_build as foo_sample {
    input:
      foo_alignment_fasta = mock_chunked.foo_aligned,
      variable_2 = cols.foo_derived_metadata,
      input_string_2 = input_string_2,
      input_file_2 = input_file_2
  }

  call foo_call_1.foo_to_id {
    input:
      foo_sequences_fasta = foo_sample.foo_subsampled_mock
  }

  call foo.foo_sites {
    input:
      variable_3 = foo_sample.foo_subsampled_mock
  }

  call foo.foo_mask_sites {
    input:
      foo_sequences = foo_sample.foo_subsampled_mock
  }

  call foo.foo_draft_tree {
    input:
      variable_4 = foo_sites.foo_masked
    }

  call foo.foo_tree {
    input:
      variable_5 = foo_draft_tree.foo_aligned_tree,
      variable_4 = foo_sites.foo_masked,
      variable_6 = cols.foo_derived_meta,
      variable_7 = input_string_1
  }

  if(defined(array_of_strings_input_1) && length(select_first([array_of_strings_input_1, []]))>0) {
    call foo.foo_traits {
      input:
        variable_8 = foo_tree.foo_refined_tree,
        variable_6 = cols.foo_derived_meta,
        variable_9 = select_first([array_of_strings_input_1, []])
    }
  }

  call foo.foo_frequencies {
    input:
      variable_8 = foo_tree.foo_refined_tree,
      variable_6 = cols.foo_derived_meta,
      variable_10 = foo_input_int_1,
      variable_11 = foo_input_int_2,
      variable_12 = "variable 12",
      variable_13 = foo_input_int_3,
      variable_14 = foo_input_int_4,
      variable_15 = "variable 15"
  }

  call foo.foo_ancestral {
    input:
      variable_8 = foo_tree.foo_refined_tree,
      variable_4 = foo_sites.foo_masked
  }

  call foo.foo_translate_tree {
    input:
      variable_8 = foo_tree.foo_refined_tree,
      variable_16 = foo_ancestral.variable_16_json,
      variable_17 = foo_defaults.foo_reference
  }

  call foo.clades_nodes {
    input:
      variable_18 = foo_tree.foo_refined_tree,
      variable_19 = foo_ancestral.variable_16_json,
      variable_20 = foo_translate_tree.variable_20_json,
      optional_input_file_1 = select_first([optional_input_file_1, foo_defaults.foo_ref_fasta]),
      optional_input_file_2 = select_first([optional_input_file_2, foo_defaults.optional_input_file_2])
  }

  call foo.foo_export_json {
    input:
      variable_8 = foo_tree.foo_refined_tree,
      variable_22 = cols.foo_derived_meta,
      variable_23 = select_first([optional_input_file_3, foo_defaults.variable_23]),
      variable_24 = select_all([foo_tree.foo_lengths,
                                 foo_traits.foo_node_json,
                                 foo_ancestral.variable_19,
                                 foo_translate_tree.variable_20,
                                 clades_nodes.foo_clade_json]),
      optional_input_file_1 = select_first([optional_input_file_1, foo_defaults.optional_input_file_1]),
      variable_26 = "variable_26"
  }

  output {
    File output_file_1 = mock.mock_combined
    File output_file_2 = mock_chunked.mock_2
    File output_file_3 = foo_sites.mock_3

    File output_file_4 = cols.foo_derived_meta
    File output_file_5 = foo_to_id.mock_4
    File output_file_6 = foo_sample.foo_subsampled_mock
    File output_file_7 = foo_mask_sites.foo_masked
    Int output_int_1 = foo_sample.mock_5
    Map[String, Int] output_map_string_int_1 = foo_sample.mock_6

    File output_file_8 = foo_draft_tree.foo_aligned_tree
    File output_file_9 = foo_tree.foo_refined_tree
    Array[File] output_array_of_files_1 = select_all([
                                               foo_tree.foo_lengths,
                                               foo_traits.foo_node_json,
                                               foo_ancestral.variable_19,
                                               foo_translate_tree.variable_20,
                                               clades_nodes.foo_clade_json])
    File output_file_10 = foo_frequencies.variable_24
    File output_file_11 = foo_export_json.output_file_11
    File output_file_12 = foo_export_json.mock_7
  }

       }

