version 1.0

workflow no_tasks {
  input {
    String name
    String id
  }

  output {
    Int x = 2
    Int y = 4
  }
}
