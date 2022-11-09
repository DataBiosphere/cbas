version 1.0

task mock_task_A {
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
    date '+%s' > mock_task_A_digest.txt
    echo 'Inputs:' >> mock_task_A_digest.txt
    
    echo 'input_file_1: ~{input_file_1}' >> mock_task_A_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_A_digest.txt
    echo 'input_file_optional: ~{input_file_optional}' >> mock_task_A_digest.txt
    echo 'input_string_default_1: ~{input_string_default_1}' >> mock_task_A_digest.txt
    echo 'input_string_optional: ~{input_string_optional}' >> mock_task_A_digest.txt
    echo 'input_bool_default: ~{input_bool_default}' >> mock_task_A_digest.txt
    echo 'input_int_optional: ~{input_int_optional}' >> mock_task_A_digest.txt
    echo 'input_string_default_2: ~{input_string_default_2}' >> mock_task_A_digest.txt
    echo 'input_string_default_3: ~{input_string_default_3}' >> mock_task_A_digest.txt
  }

  output {
    File   output_file_1   = "mock_task_A_digest.txt"
    File   output_file_2   = "mock_task_A_digest.txt"
    File   output_file_3   = "mock_task_A_digest.txt"
    File   output_file_4   = "mock_task_A_digest.txt"
    File   output_file_5   = "mock_task_A_digest.txt"
    File   output_file_6   = "mock_task_A_digest.txt"
    File   output_file_7   = "mock_task_A_digest.txt"
    Int    output_int_1    = variable_int
    Int    output_int_2    = variable_int
    Int    output_int_3    = variable_int
    Float  output_float_1  = variable_float
    Float  output_float_2  = variable_float
    Int    output_int_4    = variable_int
    Int    output_int_5    = variable_int
    String output_string_1 = "output string 1"
    String output_string_2 = "output string 2"
  }
}

task mock_task_B {
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
  Int variable_int = 99

  command {
    date '+%s' > mock_task_B_digest.txt
    echo 'Inputs:' >> mock_task_B_digest.txt
    
    echo 'input_file: ~{input_file}' >> mock_task_B_digest.txt
    echo 'input_file_optional: ~{input_file_optional}' >> mock_task_B_digest.txt
    echo 'input_int_optional_1: ~{input_int_optional_1}' >> mock_task_B_digest.txt
    echo 'input_int_optional_2: ~{input_int_optional_2}' >> mock_task_B_digest.txt
    echo 'input_int_default: ~{input_int_default}' >> mock_task_B_digest.txt
    echo 'input_int_optional_3: ~{input_int_optional_3}' >> mock_task_B_digest.txt
  }

  output {
    File   output_file   = "mock_task_B_digest.txt"
    Float  output_float  = variable_float
    Int    output_int    = variable_int
    String output_string = "a man a plan a canal panama"
  }
}

task mock_task_C {
  meta {
    description: "a mock task"
  }

  input {
    Array[File]+ input_file_array
    String?      input_string_optional
    File?        input_file_optional
    String       input_string
    String       input_string_default = "mock task C default string"
  }

  command {
    date '+%s' > mock_task_C_digest.txt
    echo 'Inputs:' >> mock_task_C_digest.txt
    
    echo 'input_string_optional: ~{input_string_optional}' >> mock_task_C_digest.txt
    echo 'input_file_optional: ~{input_file_optional}' >> mock_task_C_digest.txt
    echo 'input_string: ~{input_string}' >> mock_task_C_digest.txt
    echo 'input_string_default: ~{input_string_default}' >> mock_task_C_digest.txt
  }

  output {
    File   output_file   = "mock_task_C_digest.txt"
    String output_string = "was it a bar or a bat I saw?"
  }
}

task mock_task_D {
  input {
    File      input_file_1
    File      input_file_2

    String    input_string_default_1 = "mock task D first default string"
    String    input_string_default_2 = "mock task D second default string"
  }

  command {
    date '+%s' > mock_task_D_digest.txt
    echo 'Inputs:' >> mock_task_D_digest.txt
    
    echo 'input_file_1: ~{input_file_1}' >> mock_task_D_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_D_digest.txt
    echo 'input_string_default_1: ~{input_string_default_1}' >> mock_task_D_digest.txt
    echo 'input_string_default_2: ~{input_string_default_2}' >> mock_task_D_digest.txt
  }

  output {
    File   output_file   = "mock_task_D_digest.txt"
    String output_string = "Mr. Owl ate my metal worm"
  }
}

task mock_task_E {
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
    String input_string_default = "a default input string for mock task E"
  }

  command {
    date '+%s' > mock_task_E_digest.txt
    echo 'Inputs:' >> mock_task_E_digest.txt
    
    echo 'input_file_1: ~{input_file_1}' >> mock_task_E_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_E_digest.txt
    echo 'input_file_optional: ~{input_file_optional}' >> mock_task_E_digest.txt
    echo 'input_string_optional: ~{input_string_optional}' >> mock_task_E_digest.txt
    echo 'input_int_optional_1: ~{input_int_optional_1}' >> mock_task_E_digest.txt
    echo 'input_int_optional_2: ~{input_int_optional_2}' >> mock_task_E_digest.txt
    echo 'input_int_optional_3: ~{input_int_optional_3}' >> mock_task_E_digest.txt
    echo 'input_int_optional_4: ~{input_int_optional_4}' >> mock_task_E_digest.txt
    echo 'input_string_default: ~{input_string_default}' >> mock_task_E_digest.txt
  }

  output {
    File output_file_1 = "mock_task_E_digest.txt"
    File output_file_2 = "mock_task_E_digest.txt"
    File output_file_3 = "mock_task_E_digest.txt"
    File output_file_4 = "mock_task_E_digest.txt"
    File output_file_5 = "mock_task_E_digest.txt"
  }
}

task mock_task_F {
  input {
    File   input_file_1
    File   input_file_2
    String input_string_default_1 = "the first default string in mock task F"
    Int    input_int_default = 4
    String input_string_default_2 = "the second default string in mock task F"
  }
  
  command {
    date '+%s' > mock_task_F_digest.txt
    echo 'Inputs:' >> mock_task_F_digest.txt
    
    echo 'input_file_1: ~{input_file_1}' >> mock_task_F_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_F_digest.txt
    echo 'input_string_default_1: ~{input_string_default_1}' >> mock_task_F_digest.txt
    echo 'input_int_default: ~{input_int_default}' >> mock_task_F_digest.txt
    echo 'input_string_default_2: ~{input_string_default_2}' >> mock_task_F_digest.txt
  }
  
  output {
    File   output_file   = "mock_task_F_digest.txt"
    Int    output_int_1  = 101
    Int    output_int_2  = 102
    Int    output_int_3  = 103
    Int    output_int_4  = 104
    Int    output_int_5  = 105
    String output_string = "mock test F output string"
  }
}

task mock_task_G {
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
    date '+%s' > mock_task_G_digest.txt
    echo 'Inputs:' >> mock_task_G_digest.txt
    
    echo 'input_file: ~{input_file}' >> mock_task_G_digest.txt
    echo 'input_string: ~{input_string}' >> mock_task_G_digest.txt
    echo 'input_bool_default_1: ~{input_bool_default_1}' >> mock_task_G_digest.txt
    echo 'input_bool_default_2: ~{input_bool_default_2}' >> mock_task_G_digest.txt
    echo 'input_bool_default_3: ~{input_bool_default_3}' >> mock_task_G_digest.txt
    echo 'input_string_default: ~{input_string_default}' >> mock_task_G_digest.txt
    echo 'input_int_default_1: ~{input_int_default_1}' >> mock_task_G_digest.txt
    echo 'input_int_default_2: ~{input_int_default_2}' >> mock_task_G_digest.txt
    echo 'input_int_default_3: ~{input_int_default_3}' >> mock_task_G_digest.txt
    echo 'input_int_optional_1: ~{input_int_optional_1}' >> mock_task_G_digest.txt
    echo 'input_int_optional_2: ~{input_int_optional_2}' >> mock_task_G_digest.txt
    echo 'input_int_optional_3: ~{input_int_optional_3}' >> mock_task_G_digest.txt
    echo 'input_int_optional_4: ~{input_int_optional_4}' >> mock_task_G_digest.txt
    echo 'input_string_optional_1: ~{input_string_optional_1}' >> mock_task_G_digest.txt
    echo 'input_string_optional_2: ~{input_string_optional_2}' >> mock_task_G_digest.txt
  }

  output {
    File   output_file_1  = "mock_task_G_digest.txt"
    File   output_file_2  = "mock_task_G_digest.txt"
    Int    output_int_1   = 1
    Int    output_int_2   = 2
    Int    output_int_3   = 3
    Float  output_float_1 = 4.0
    Float  output_float_2 = 5.0
    String output_string  = "hello world"
  }
}

task mock_task_H {

  input {
    File     input_file_1
    File     input_file_2
    String   input_string

    Boolean  input_boolean_default = false
    Float    input_float_default = 0.5
    Int      input_int_default = 3

    Int?     input_int_optional
    String   input_string_default = "a default string for mock task H"
  }

  command {
    date '+%s' > mock_task_H_digest.txt
    echo 'Inputs:' >> mock_task_H_digest.txt
    
    echo 'input_file_1: ~{input_file_1}' >> mock_task_H_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_H_digest.txt
    echo 'input_string: ~{input_string}' >> mock_task_H_digest.txt
    echo 'input_boolean_default: ~{input_boolean_default}' >> mock_task_H_digest.txt
    echo 'input_float_default: ~{input_float_default}' >> mock_task_H_digest.txt
    echo 'input_int_default: ~{input_int_default}' >> mock_task_H_digest.txt
    echo 'input_int_optional: ~{input_int_optional}' >> mock_task_H_digest.txt
    echo 'input_string_default: ~{input_string_default}' >> mock_task_H_digest.txt
  }

  output {
      File   output_file_1 = "mock_task_H_digest.txt"
      File   output_file_2 = "mock_task_H_digest.txt"
      Int    output_int_1  = 202
      Int    output_int_2  = 302
      Int    output_int_3  = 402
      Int    output_int_4  = 502
      String output_string = "hello world"
  }
}

task delay_completion {
  input {
    String timestamp
  }

  command {
    target_epoch=$(date --universal --date="~{timestamp}" +"%s")
    current_epoch=$(date +%s)
    sleep_seconds=$(($target_epoch - $current_epoch))
    echo "target epoch: $target_epoch"
    echo "current epoch: $current_epoch"
    echo "sleeping for $sleep_seconds seconds"
    
    if [ $target_epoch \> $current_epoch ];
      then
        sleep $sleep_seconds
      else
        echo "target completion date ~{timestamp} is in the past";
      fi;
  }

  runtime {
    docker: "ubuntu:latest"
    cpu: 2
    memory: "3 GB"
    disks: "local-disk 10 LOCAL"
    maxRetries: 2
  }
}

workflow assemble_refbased_mock {

    meta {
        description: "A test workflow, simulating inputs and outputs of `assemble_refbased`, to help build out CBAS functionality"
    }

    parameter_meta {
      delay_completion_until: 'a UTC timestamp in a format like this: 11/31/2022 14:00:01'
    }

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

        String       delay_completion_until = "11/09/2022 14:45:00"
    }

    scatter(file_ in input_file_array) {
        call mock_task_A as call_1 {
            input:
                input_file_1          = file_,
                input_file_2          = file_,
                input_file_optional   = file_,
                input_string_optional = input_string_default,
                input_int_optional    = input_int_default
        }
        call mock_task_B as call_2 {
            input:
                input_file            = file_,
                input_file_optional   = file_
        }
    }

    call mock_task_C as call_3 {
        input:
            input_file_array = input_file_array,
            input_string  = "hello world",
    }

    call mock_task_D as call_4 {
        input:
            input_file_1 = input_file,
            input_file_2 = input_file
    }

    call mock_task_E as call_5 {
        input:
            input_file_1         = input_file,
            input_file_2         = input_file,
            input_file_optional  = input_file,
            input_int_optional_1 = 3
    }

    call mock_task_F as call_6 {
        input:
            input_file_1         = input_file,
            input_file_2         = input_file,
            input_int_default    = input_int_default
    }

    call mock_task_G as call_7 {
        input:
            input_file   = input_file,
            input_string = "hello world"
    }

    call mock_task_H as call_8 {
        input:
            input_file_1 = input_file,
            input_file_2 = input_file,
            input_string = "hello world"
    }

    scatter(reads_unmapped_bam in input_file_array) {
      call mock_task_A as call_9 {
        input:
          input_file_1          = input_file,
          input_file_2          = input_file,
          input_file_optional   = input_file,
          input_string_optional = input_string_default,
          input_int_optional    = input_int_default
      }
    }

    call mock_task_C as call_10 {
      input:
          input_file_array = input_file_array,
          input_string  = "hello world"
    }

    call mock_task_D as call_11 {
      input:
          input_file_1 = input_file,
          input_file_2 = input_file
    }

    call mock_task_G as call_12 {
      input:
        input_file   = input_file,
        input_string = "hello world"
    }

    call delay_completion {
      input:
        timestamp = delay_completion_until
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
