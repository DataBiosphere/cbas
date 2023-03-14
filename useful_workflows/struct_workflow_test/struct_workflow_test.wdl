version 1.0

struct Person {
  String name
  Int age
}

task task_a {
  meta {
    description: ""
  }

  input {
    Person a
  }
  
  command {
    echo "hello my name is ${a.name} and I am ${a.age} years old"
  }

  runtime {
    docker: "ubuntu:latest"
    cpu: 2
    memory: "3 GB"
    disks: "local-disk 10 LOCAL"
    maxRetries: 2
  }
}

workflow myWorkflow {
  call task_a
}
