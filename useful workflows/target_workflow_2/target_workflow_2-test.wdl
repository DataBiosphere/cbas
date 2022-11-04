version 1.0

workflow assemble_refbased {

    meta {
        description: "A test workflow, simulating inputs and outputs of `assemble_refbased`, to help build out CBAS functionality"
    }

    parameter_meta {}

    input {
        Array[File]+ file_array
        File         required_file
        String       first_file_name = basename(file_array[0], '.txt')

        String       optional_string = "foo"
        File?        optional_file_1
        Int          optional_int = 3
        Float        optional_float = 0.75
        Boolean      optional_bool = false
        File?        optional_file_2
    }


    output {
        File                        result_file_1           = file_array[0]
        File                        result_file_2           = file_array[0]
        Int                         result_int_1            = optional_int
        Int                         result_int_2            = optional_int
        Int                         result_int_3            = optional_int
        Float                       result_float_1          = optional_float
        Int                         result_int_4            = optional_int
        Int                         result_int_5            = optional_int
        
        Array[Int]                  result_int_array_1      = [optional_int]
        Array[Float]                result_float_array      = [optional_float]
        Array[Map[String,String]]   array_of_maps_of_str    = [{required_file: optional_string}]
        Array[Array[String]]        array_of_arrays_of_str  = [[optional_string]]
        
        Int                         result_int_6            = optional_int
        Int                         result_int_7            = optional_int
        Int                         result_int_8            = optional_int
        Int                         result_int_9            = optional_int
        Int                         result_int_10           = optional_int
        File                        result_file_3           = required_file
        
        Array[File]                 result_file_array_1     = [required_file]
        Array[Int]                  result_int_array_2      = [optional_int]
        Array[Int]                  result_int_array_2      = [optional_int]
        Array[File]                 result_file_array_2     = [required_file]
        
        File                        result_file_4           = required_file
        File                        result_file_5           = required_file
        File                        result_file_6           = required_file
        Int                         result_int_11           = optional_int
        Int                         result_int_12           = optional_int
        Float                       result_float_2          = optional_float
        File                        result_file_7           = required_file
        
        File                        result_file_8           = required_file
        File                        result_file_9           = required_file
        File                        result_file_10          = required_file
        File                        result_file_11          = required_file
        File                        result_file_12          = required_file

        Array[File]                 result_file_array_3     = [required_file]

        File                        result_file_13          = required_file
        File                        result_file_14          = required_file
        File                        result_file_15          = required_file
        Int                         result_int_13           = optional_int
        Int                         result_int_14           = optional_int
        Float                       result_float_3          = optional_float
        Float                       result_float_4          = optional_float
        File                        result_file_16          = required_file
        
        String                      result_string_1         = optional_string
        String                      result_string_2         = optional_string
        String                      result_string_3         = optional_string
    }

}
