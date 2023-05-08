version 1.0

workflow test_all_engine_functions {
    call create_stdout

    output {
        File my_stdout = create_stdout.my_stdout
    }
}

task create_stdout {
    command {
        echo "Hello World!"
    }
    output {
        File my_stdout = stdout()
    }
}

