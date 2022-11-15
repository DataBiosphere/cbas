'''
A script for converting the output of womtool's `inputs` command to a CBAS input definitions file.

This script was written (quickly) to accelerate the test and development of CBAS features,
and should not be considered to be actively maintained. Use at your own risk.

'''

import argparse
import json
import re


def get_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "womtool_file",
        type=str,
        help="Path to a JSON file generated from womtool's `inputs` command"
    )
    parser.add_argument(
        "cbas_file",
        type=str,
        help="The name of a file to write the resulting CBAS input definitions"
    )
    return parser.parse_args()


def main():
    args = get_args()
    
    with open(args.womtool_file) as f:
        womtool_result = json.load(f)

        cbas_definition = {k: womtool_to_cbas(k, v) for k, v in womtool_result.items()}

    with open(args.cbas_file, 'w') as f:
        json.dump(cbas_definition, f, indent=2)


PRIMITIVES = ['Int', 'String', 'Float', 'Boolean', 'File']
ALL_TYPES = PRIMITIVES + ['Array', 'Map']


def parse_default_value(womtool_string):
    default_value_matches = re.search('\(optional, default = (.*)\)', womtool_string)
    default_value = default_value_matches.group(1) if default_value_matches else None
    return default_value


def parse_source_spec(input_name):
    return {
        "type": "record_lookup",
        "record_attribute": input_name.replace('.', '_')
    }


def parse_input_type(womtool_string):
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
        f"{outer_type_key}_type": outer_type if not inner_type else parse_input_type(inner_type)
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
    

def womtool_to_cbas(input_name, womtool_string):
    return {
        "input_name": input_name,
        "input_type": parse_input_type(womtool_string),
        "source": parse_source_spec(input_name)
    }

if __name__ == "__main__":
    main()
