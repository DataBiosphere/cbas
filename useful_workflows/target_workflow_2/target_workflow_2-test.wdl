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

  Float declared_float = 0.67
  Int declared_int = 5

  command {
    echo 'output file 1' > output-file-1.txt
    echo 'output file 2' > output-file-2.txt
    echo 'output file 3' > output-file-3.txt
    echo 'output file 4' > output-file-4.txt
    echo 'output file 5' > output-file-5.txt
    echo 'output file 6' > output-file-6.txt
    echo 'output file 7' > output-file-7.txt

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
    File   output_file_1   = "output-file-1.txt"
    File   output_file_2   = "output-file-2.txt"
    File   output_file_3   = "output-file-3.txt"
    File   output_file_4   = "output-file-4.txt"
    File   output_file_5   = "output-file-5.txt"
    File   output_file_6   = "output-file-6.txt"
    File   output_file_7   = "output-file-7.txt"
    Int    output_int_1    = declared_int
    Int    output_int_2    = declared_int
    Int    output_int_3    = declared_int
    Float  output_float_1  = declared_float
    Float  output_float_2  = declared_float
    Int    output_int_4    = declared_int
    Int    output_int_5    = declared_int
    String output_string_1 = "output string 1"
    String output_string_2 = "output string 2"
    File   output_digest   = "mock_task_A_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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

  Float declared_float = 0.82
  Int declared_int = 99

  command {
    echo 'output file' > output-file.txt

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
    File   output_file   = "output-file.txt"
    Float  output_float  = declared_float
    Int    output_int    = declared_int
    String output_string = "a man a plan a canal panama"
    File   output_digest = "mock_task_B_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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
    echo 'output file' > output-file.txt

    date '+%s' > mock_task_C_digest.txt
    echo 'Inputs:' >> mock_task_C_digest.txt

    echo 'input_string_optional: ~{input_string_optional}' >> mock_task_C_digest.txt
    echo 'input_file_optional: ~{input_file_optional}' >> mock_task_C_digest.txt
    echo 'input_string: ~{input_string}' >> mock_task_C_digest.txt
    echo 'input_string_default: ~{input_string_default}' >> mock_task_C_digest.txt
  }

  output {
    File   output_file     = "output-file.txt"
    File   output_digest   = "mock_task_C_digest.txt"
    String output_string   = "was it a bar or a bat I saw?"
  }

  runtime {
    docker: "ubuntu:latest"
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
    echo 'output file' > output-file.txt

    date '+%s' > mock_task_D_digest.txt
    echo 'Inputs:' >> mock_task_D_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_D_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_D_digest.txt
    echo 'input_string_default_1: ~{input_string_default_1}' >> mock_task_D_digest.txt
    echo 'input_string_default_2: ~{input_string_default_2}' >> mock_task_D_digest.txt
  }

  output {
    File   output_file   = "output-file.txt"
    String output_string = "Mr. Owl ate my metal worm"
    File   output_digest = "mock_task_D_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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
    echo 'output file 1' > output-file-1.txt
    echo 'output file 2' > output-file-2.txt
    echo 'output file 3' > output-file-3.txt
    echo 'output file 4' > output-file-4.txt
    echo 'output file 5' > output-file-5.txt

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
    File output_file_1 = "output-file-1.txt"
    File output_file_2 = "output-file-2.txt"
    File output_file_3 = "output-file-3.txt"
    File output_file_4 = "output-file-4.txt"
    File output_file_5 = "output-file-5.txt"
    File output_digest = "mock_task_E_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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
    echo 'output file' > output-file.txt
    date '+%s' > mock_task_F_digest.txt
    echo 'Inputs:' >> mock_task_F_digest.txt

    echo 'input_file_1: ~{input_file_1}' >> mock_task_F_digest.txt
    echo 'input_file_2: ~{input_file_2}' >> mock_task_F_digest.txt
    echo 'input_string_default_1: ~{input_string_default_1}' >> mock_task_F_digest.txt
    echo 'input_int_default: ~{input_int_default}' >> mock_task_F_digest.txt
    echo 'input_string_default_2: ~{input_string_default_2}' >> mock_task_F_digest.txt
  }

  output {
    File   output_file   = "output-file.txt"
    Int    output_int_1  = 101
    Int    output_int_2  = 102
    Int    output_int_3  = 103
    Int    output_int_4  = 104
    Int    output_int_5  = 105
    String output_string = "mock test F output string"
    File   output_digest = "mock_task_F_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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

    echo 'output file 1' > output-file-1.txt
    echo 'output file 2' > output-file-2.txt

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
    File   output_file_1  = "output-file-1.txt"
    File   output_file_2  = "output-file-1.txt"
    Int    output_int_1   = 1
    Int    output_int_2   = 2
    Int    output_int_3   = 3
    Float  output_float_1 = 4.0
    Float  output_float_2 = 5.0
    String output_string  = "hello world"
    File   output_digest  = "mock_task_G_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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
    echo 'output file 1' > output-file-1.txt
    echo 'output file 2' > output-file-2.txt

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
    File   output_file_1 = "output-file-1.txt"
    File   output_file_2 = "output-file-1.txt"
    Int    output_int_1  = 202
    Int    output_int_2  = 302
    Int    output_int_3  = 402
    Int    output_int_4  = 502
    String output_string = "hello world"
    File   output_digest = "mock_task_H_digest.txt"
  }

  runtime {
    docker: "ubuntu:latest"
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

  Int declared_int_1 = 9999
  Int declared_int_2 = -999

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

    Map[String,String] scattered_stringtostring_map = {
                                                        'foo': call_1.output_file_1,
                                                        'bar': call_1.output_file_2,
                                                        'baz': call_1.output_string_1
                                                      }
    Array[String] scattered_string_array = [
                                           call_2.output_string,
                                           "a second thing",
                                           "a third thing"
                                           ]
    Array[Int] scattered_int_array = [
                                     call_1.output_int_1,
                                     call_1.output_int_2,
                                     call_1.output_int_3,
                                     call_1.output_int_4,
                                     call_1.output_int_5
                                     ]
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
    File                        output_file_1           = call_1.output_file_1[0]
    File                        output_file_2           = call_2.output_file[0]
    Int                         output_int_1            = call_6.output_int_1
    Int                         output_int_2            = call_6.output_int_2
    Int                         output_int_3            = call_6.output_int_3
    Float                       output_float_1          = call_7.output_float_1
    Int                         output_int_4            = call_6.output_int_4
    Int                         output_int_5            = call_6.output_int_5

    Array[Int]                  output_int_array_1      = [
                                                          call_7.output_int_1, call_7.output_int_2
                                                          ]
    Array[Float]                output_float_array      = [call_7.output_float_1]
    Array[Map[String,String]]   array_of_maps_of_str    = scattered_stringtostring_map
    Array[Array[String]]        array_of_arrays_of_str  = scattered_string_array

    Int                         output_int_6            = call_6.output_int_1
    Int                         output_int_7            = call_6.output_int_2
    Int                         output_int_8            = call_6.output_int_3
    Int                         output_int_9            = scattered_int_array[0][0]
    Int                         output_int_10           = scattered_int_array[0][1]
    File                        output_file_3           = call_1.output_digest[0]

    Array[File]                 output_file_array_1     = call_1.output_file_1
    Array[Int]                  output_int_array_2      = scattered_int_array[0]
    Array[Int]                  output_int_array_3      = [1, 2, 3, 4, 5, 6]
    Array[File]                 output_file_array_2     = [
                                                          call_1.output_digest[0],
                                                          call_1.output_file_1[0]
                                                          ]

    File                        output_file_4           = call_3.output_file
    File                        output_file_5           = call_4.output_file
    File                        output_file_6           = call_5.output_file_1
    Int                         output_int_11           = scattered_int_array[0][2]
    Int                         output_int_12           = scattered_int_array[0][3]
    Float                       output_float_2          = 0.123
    File                        output_file_7           = call_5.output_file_2

    File                        output_file_8           = call_5.output_file_3
    File                        output_file_9           = call_5.output_file_4
    File                        output_file_10          = call_5.output_file_5
    File                        output_file_11          = call_12.output_file_1
    File                        output_file_12          = call_12.output_file_2

    Array[File]                 output_file_array_3     = [
                                                          call_3.output_digest,
                                                          call_4.output_digest,
                                                          call_4.output_digest
                                                          ]

    File                        output_file_13          = call_5.output_digest
    File                        output_file_14          = call_6.output_digest
    File                        output_file_15          = call_7.output_digest
    Int                         output_int_13           = declared_int_1
    Int                         output_int_14           = declared_int_2
    Float                       output_float_3          = 4.567
    Float                       output_float_4          = 8.910
    File                        output_file_16          = call_8.output_digest

    String                      output_string_1         = "north"
    String                      output_string_2         = "south"
    String                      output_string_3         = "east"
  }

}
