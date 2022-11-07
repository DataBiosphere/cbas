version 1.0

task align_reads_mock {
  meta {
    description: "A mock task"
  }

  input {
    File     input_file_1
    File     input_file_2

    File?    input_file_optional

    String   input_string_default_1 = "this is a default string"
    String?  input_string_optional
    Boolean? input_bool_default = false

    Int?     input_int_optional
    String   input_string_default_2 = "my-docker-image.123"

    String   input_string_default_3 = "this is a second default string"
  }

  Float variable_float = 0.67
  Int variable_int = 5

  command {
    echo "align_reads_mock"
  }

  output {
    File   output_file_1   = input_file_1
    File   output_file_2   = input_file_1
    File   output_file_3   = input_file_1
    File   output_file_4   = input_file_1
    File   output_file_5   = input_file_1
    File   output_file_6   = input_file_1
    File   output_file_7   = input_file_1
    Int    output_int_1    = variable_int
    Int    output_int_2    = variable_int
    Int    output_int_3    = variable_int
    Float  output_float_1  = variable_float
    Float  output_float_2  = variable_float
    Int    output_int_4    = variable_int
    Int    output_int_5    = variable_int
    String output_string_1 = input_string_default_1
    String output_string_2 = input_string_default_2
  }
}

task ivar_trim_mock {
  meta {
    description: "A mock task"
  }

  input {
    File   input_file
    File?  input_file_optional
    Int?   input_int_optional_1
    Int?   input_int_optional_2
    Int?   input_int_default = 1
    Int?   input_int_optional_3
    
    Int?   input_int_optional_4
    String input_string_default = "ivar trim mock string"
  }

  Float variable_float = 0.82

  command {
    echo "ivar_trim_mock"
  }

  output {
    File   output_file   = input_file
    Float  output_float  = variable_float
    Int    output_int    = input_int_default
    String output_string = input_string_default
  }
}

task merge_and_reheader_bams_mock {
  meta {
    description: "a mock task"
  }

  input {
    Array[File]+ input_file_array
    String?      input_string_optional
    File?        input_file_optional
    String       input_string

    String       input_string_default = "a default string"
  }

  command {
    echo "merge_and_reheader_bams_mock"
  }

  output {
    File   output_file   = input_file_array[0]
    String output_string = input_string
  }
}

task lofreq_mock {
  input {
    File      input_file_1
    File      input_file_2

    String    input_string_default_1 = "some default string"
    String    input_string_default_2 = "quay.io/biocontainers/lofreq:2.1.5--py38h588ecb2_4"
  }

  command {
    echo "lofreq_mock"
  }

  output {
    File   output_file   = input_file_1
    String output_string = input_string_default_1
  }
}

task alignment_metrics_mock {
  meta {}

  input {
    File   input_file_1
    File   input_file_2
    File?  input_file_optional
    String? input_string_optional
    Int?   input_int_optional_1
    Int?   input_int_optional_2
    Int?   input_int_optional_3

    Int?   input_int_optional_4
    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }

  command {
    echo "alignment_metrics_mock"
  }

  output {
    File output_file_1 = input_file_1
    File output_file_2 = input_file_2
    File output_file_3 = input_file_1
    File output_file_4 = input_file_2
    File output_file_5 = input_file_1
  }
}

task run_discordance_mock {
  input {
    File   input_file_1
    File   input_file_2
    String input_string_default = "run"
    Int    input_int_default = 4

    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }
  
  command {
    echo "run_discordance_mock"
  }
  
  output {
    File   output_file   = input_file_1
    Int    output_int_1  = input_int_default
    Int    output_int_2  = input_int_default
    Int    output_int_3  = input_int_default
    Int    output_int_4  = input_int_default
    Int    output_int_5  = input_int_default
    String output_string = input_int_default
  }
}

task plot_coverage_mock {
  input {
    File    input_file
    String  input_string

    Boolean input_bool_default_1  = false
    Boolean input_bool_default_2  = false
    Boolean input_bool_default_3  = false
    String? input_string_default  = "max" # max or min

    Int? input_int_default_1      = 1100
    Int? input_int_default_2      = 850
    Int? input_int_default_3      = 100

    Int? input_int_optional_1
    Int? input_int_optional_2
    Int? input_int_optional_3
    Int? input_int_optional_4
    String? input_string_optional_1 # of the form "min max" (ints, space between)
    String? input_string_optional_2 # of the form "min max" (ints, space between)

    String docker = "quay.io/broadinstitute/viral-core:2.1.33"
  }

  command {
    echo "plot_coverage_mock"
  }

  output {
    File   output_file_1  = input_file
    File   output_file_2  = input_file
    Int    output_int_1   = 1
    Int    output_int_2   = 2
    Int    output_int_3   = 3
    Float  output_float_1 = 4.0
    Float  output_float_2 = 5.0
    String output_string  = "hello world"
  }
}

task refine_assembly_with_aligned_reads_mock {

  input {
    File     input_file_1
    File     input_file_2
    String   input_string

    Boolean  input_boolean_default = false
    Float    input_float_default = 0.5
    Int      input_int_default = 3

    Int?     input_int_optional
    String   docker = "quay.io/broadinstitute/viral-assemble:2.1.16.1"
  }

  command {
    echo "refine_assembly_with_aligned_reads_mock"
  }

  output {
      File   output_file_1 = input_file_1
      File   output_file_2 = input_file_2
      Int    output_int_1  = input_int_default
      Int    output_int_2  = input_int_default
      Int    output_int_3  = input_int_default
      Int    output_int_4  = input_int_default
      String output_string = "hello world"
  }
}

workflow assemble_refbased {

    meta {
        description: "A test workflow, simulating inputs and outputs of `assemble_refbased`, to help build out CBAS functionality"
    }

    parameter_meta {}

    input {
        Array[File]+ input_file_array
        File         input_file
        String       input_string_default = "a default string"

        String       input_string_optional = "foo"
        File?        input_file_optional_1
        Int          input_int_default = 3
        Float        input_float_default = 0.75
        Boolean      input_bool_default = false
        File?        input_file_optional_2
    }

    scatter(file_ in input_file_array) {
        call align_reads_mock as call_1 {
            input:
                input_file_1          = file_,
                input_file_2          = file_,
                input_file_optional   = file_,
                input_string_optional = input_string_default,
                input_int_optional    = input_int_default
        }
        call ivar_trim_mock as call_2 {
            input:
                input_file            = file_,
                input_file_optional   = file_
        }
    }

    call merge_and_reheader_bams_mock as call_3 {
        input:
            input_file_array = input_file_array,
            input_string  = "hello world",
    }

    call lofreq_mock as call_4 {
        input:
            input_file_1 = input_file,
            input_file_2 = input_file
    }

    call alignment_metrics_mock as call_5 {
        input:
            input_file_1         = input_file,
            input_file_2         = input_file,
            input_file_optional  = input_file,
            input_int_optional_1 = 3
    }

    call run_discordance_mock as call_6 {
        input:
            input_file_1         = input_file,
            input_file_2         = input_file,
            input_string_default = "some string",
            input_int_default    = input_int_default
    }

    call plot_coverage_mock as call_7 {
        input:
            input_file   = input_file,
            input_string = "hello world"
    }

    call refine_assembly_with_aligned_reads_mock as call_8 {
        input:
            input_file_1 = input_file,
            input_file_2 = input_file,
            input_string = "hello world"
    }

    scatter(reads_unmapped_bam in input_file_array) {
        call align_reads_mock as call_9 {
            input:
                input_file_1          = input_file,
                input_file_2          = input_file,
                input_file_optional   = input_file,
                input_string_optional = input_string_default,
                input_int_optional    = input_int_default
        }
    }

    call merge_and_reheader_bams_mock as call_10 {
        input:
            input_file_array = input_file_array,
            input_string  = "hello world"
    }

    call lofreq_mock as call_11 {
        input:
            input_file_1 = input_file,
            input_file_2 = input_file
    }

    call plot_coverage_mock as call_12 {
        input:
            input_file   = input_file,
            input_string = "hello world"
    }

    output {
        File                        output_file_1           = input_file_array[0]
        File                        output_file_2           = input_file_array[0]
        Int                         output_int_1            = input_int_default
        Int                         output_int_2            = input_int_default
        Int                         output_int_3            = input_int_default
        Float                       output_float_1          = input_float_default
        Int                         output_int_4            = input_int_default
        Int                         output_int_5            = input_int_default
        
        Array[Int]                  output_int_array_1      = [input_int_default]
        Array[Float]                output_float_array      = [input_float_default]
        Array[Map[String,String]]   array_of_maps_of_str    = [{input_file: input_string_optional}]
        Array[Array[String]]        array_of_arrays_of_str  = [[input_string_optional]]
        
        Int                         output_int_6            = input_int_default
        Int                         output_int_7            = input_int_default
        Int                         output_int_8            = input_int_default
        Int                         output_int_9            = input_int_default
        Int                         output_int_10           = input_int_default
        File                        output_file_3           = input_file
        
        Array[File]                 output_file_array_1     = [input_file]
        Array[Int]                  output_int_array_2      = [input_int_default]
        Array[Int]                  output_int_array_2      = [input_int_default]
        Array[File]                 output_file_array_2     = [input_file]
        
        File                        output_file_4           = input_file
        File                        output_file_5           = input_file
        File                        output_file_6           = input_file
        Int                         output_int_11           = input_int_default
        Int                         output_int_12           = input_int_default
        Float                       output_float_2          = input_float_default
        File                        output_file_7           = input_file
        
        File                        output_file_8           = input_file
        File                        output_file_9           = input_file
        File                        output_file_10          = input_file
        File                        output_file_11          = input_file
        File                        output_file_12          = input_file

        Array[File]                 output_file_array_3     = [input_file]

        File                        output_file_13          = input_file
        File                        output_file_14          = input_file
        File                        output_file_15          = input_file
        Int                         output_int_13           = input_int_default
        Int                         output_int_14           = input_int_default
        Float                       output_float_3          = input_float_default
        Float                       output_float_4          = input_float_default
        File                        output_file_16          = input_file
        
        String                      output_string_1         = input_string_optional
        String                      output_string_2         = input_string_optional
        String                      output_string_3         = input_string_optional
    }

}
