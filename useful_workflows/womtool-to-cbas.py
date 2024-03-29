'''
A script for converting the output of womtool's `inputs` command to a CBAS input definition.
This script was written (quickly) to accelerate the test and development of CBAS features,
and should not be considered to be actively maintained. Use at your own risk.

Given a WDL file, produce the JSON input to this script with the following command:

`java -jar womtool.jar inputs my-workflow.wdl`

See https://cromwell.readthedocs.io/en/stable/WOMtool/ for instructions on building `womtool.jar`.

An example JSON generated from womtool's `inputs` command:
{
  "workflow_name.call_1.input_string_required": "String",
  "workflow_name.call_2.input_string_with_default": "String (optional, default = \"hello world\")",
  "workflow_name.call_3.input_int_optional": "Int? (optional)"
}

Run `python womtool-to-cbas.py --help` for more argument details.

'''


import argparse
import json
import re


def get_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "womtool_file",
        type=str,
        help="Path to a JSON file generated from womtool's `inputs` command",
    )
    parser.add_argument(
        "--output-def",
        action="store_true",
        help="If present, the results will be formatted as output definitions",
    )

    return parser.parse_args()


def main():
    args = get_args()

    with open(args.womtool_file) as f:
        womtool_result = json.load(f)
        cbas_definition = [womtool_to_cbas(k, v, args.output_def) for k, v in womtool_result.items()]

    print(json.dumps(cbas_definition, indent=2))


PRIMITIVES = ['Int', 'String', 'Float', 'Boolean', 'File']
ALL_TYPES = PRIMITIVES + ['Array']


def parse_default_value(womtool_string):
    default_value_matches = re.search('\(optional, default = (.*)\)', womtool_string)
    default_value = default_value_matches.group(1) if default_value_matches else None
    return default_value


def parse_source_spec(name, output_def):
    record_attribute = name.replace('.', '_')
    return {
        "type": "record_lookup" if output_def == False else "record_update",
        "record_attribute": record_attribute
    }

def parse_type(womtool_string):
    # WARNING: Doesn't support type "Map" yet.
    type_matches = re.search(
        f'({ "|".join(ALL_TYPES) })(\[(.+)?\]\+?)?',
        womtool_string
    )

    if not type_matches:
        raise ValueError(
            f"String '{womtool_string}' could not be parsed into a type specification."
        )

    outer_type, inner_type_opts, inner_type = type_matches.groups()
    outer_type_key = outer_type.lower() if not outer_type in PRIMITIVES else "primitive"

    type_spec = {
        "type": outer_type_key,
        f"{outer_type_key}_type": outer_type if not inner_type else parse_type(inner_type)
    }

    # reminder: the parentheses around '(optional)' denote a capture group, not literal parentheses
    optional_match = re.search('(optional)', womtool_string)
    if optional_match:
        type_spec = {
            "type": "optional",
            "optional_type": type_spec
        }

    if outer_type_key == 'array':
        if not (inner_type_opts and inner_type):
            raise ValueError(
                f"String '{womtool_string}' is missing an inner type."
            )
        type_spec["non_empty"] = inner_type_opts.endswith('+')
    return type_spec


def womtool_to_cbas(name, womtool_string, output_def=False):
    mode = "input" if not output_def else "output"
    definition = {
        f"{mode}_name": name,
        f"{mode}_type": parse_type(womtool_string),
        "source" if mode == "input" else "destination": parse_source_spec(name, output_def),
    }

    return definition


if __name__ == "__main__":
    main()
