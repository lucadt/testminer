'''
Created on Feb 7, 2017

@author: Luca Della Toffola
'''

import argparse
import logging
import os
import json

with open("words_to_filter.txt") as wf:
    words_to_filter = [line.strip() for line in wf.readlines()]

with open("packages_to_filter.txt") as wf:
    packages_to_filter = [line.strip() for line in wf.readlines()]


def get_args():
    parser = argparse.ArgumentParser(description='TestMiner dataset pre-processor')

    parser.add_argument(
        '--input',
        dest='input',
        type=str,
        default=None,
        help='specifies the resulting directory with the parsed of data')

    parser.add_argument(
        '--output',
        dest='output',
        type=str,
        default=None,
        help='specifies the directory where to save the elaborated data')

    parser.add_argument(
        '--type',
        dest='type',
        type=str,
        default="string",
        help='specifies the primitive type to export (e.g., string -> only type supported)')

    parser.add_argument(
        '--filter',
        dest='filter',
        action="store_true",
        default=False,
        help='specifies to filter tuples that are part of testing set')

    parser.add_argument(
        '--use-generics',
        dest='use_generics',
        action="store_true",
        default=False,
        help='specifies to include generics of the method signature')

    return parser.parse_args()


def logger():
    return logging.getLogger('preprocess')


def setup_logger():
    logging_level = logging.DEBUG
    log_format = '%(asctime)s-%(levelname)s-%(name)s-%(message)s'
    logging.basicConfig(
        level=logging_level,
        format=log_format,
        filename='preprocess.log',
        filemode='a+')
    console = logging.StreamHandler()
    console.setLevel(logging_level)
    console.setFormatter(logging.Formatter(log_format))
    logging.getLogger('').addHandler(console)


def is_signature_to_filter(signature):
    if 'test' in signature.lower():
        return True

    if 'assert' in signature.lower():
        return True

    return any([signature.startswith(package) for package in packages_to_filter])


def is_value_to_filter(value):
    if isinstance(value, basestring):

        if len(value) == 0:
            return True

        if len(value) == 1 and value[0].lower().isalpha():
            return True

        if value in words_to_filter:
            return True

        return False

    else:
        return True


def signature(json_call, use_generics=False):
    json_target_type = json_call['target_type']
    class_name = json_target_type['name']
    if use_generics:
        class_type_args = json_target_type['type_args']
        if len(class_type_args) > 0:
            class_name = '{0:s}<{1:s}>'.format(class_name, ','.join(class_type_args))
    json_target_method = json_call['target_method']
    method_name = json_target_method['name']
    if use_generics:
        method_type_args = json_target_method['type_args']
        if len(method_type_args) > 0:
            method_name = '{0:s}<{1:s}>'.format(method_name, ','.join(method_type_args))

    method_params = ','.join(json_target_method['params'])
    method_name = '{0:s}({1:s})'.format(method_name, method_params)
    return '{0:s}.{1:s}'.format(class_name, method_name)


def process_chunks(chunks_path, keep_tuple, process_tuple):
    storage = {}
    nb_of_files = 0
    for path, dirs, files in os.walk(chunks_path):
        for file_name in files:
            nb_of_files += 1
            with open(os.path.join(path, file_name)) as chunk_file:
                logger().info('Elaborating: {0:d} -- {1:s}'.format(nb_of_files, file_name))
                try:
                    json_chunk = json.load(chunk_file)
                    for json_type in json_chunk:
                        if 'class' in json_type:
                            for json_method in json_type['methods']:
                                for json_tuple in json_method['tuples']:
                                    if keep_tuple(json_tuple):
                                        process_tuple(storage, json_tuple)
                except Exception as e:
                    import traceback, sys
                    print e
                    traceback.print_exc(file=sys.stdout)
                    logger().info('Error reading file: {0:d} -- {1:s}'.format(nb_of_files, file_name))

    return storage

def main():
    args = get_args()

    setup_logger()

    type_to_consider = args.type
    use_generics = args.use_generics

    if args.filter:
        filter_signature = is_signature_to_filter
    else:
        def everything_goes(method_signature):
            return False
        filter_signature = everything_goes

    def keep_tuple(json_tuple):
        return json_tuple['type'] == 'call' and len(json_tuple['values']) > 0

    def process_tuple(storage, json_tuple):
        values = []
        for typed_value in json_tuple["values"]:
            if typed_value["type"] == type_to_consider:
                typed_value["type"] = typed_value["type"].encode('utf-8').strip()
                if typed_value["type"] == "string":
                    typed_value["value"] = typed_value["value"].encode('utf-8').strip()
                    values.append(typed_value["value"])

        if len(values) > 0:
            method_signature = signature(json_tuple, use_generics)
            if not filter_signature(method_signature):
                if method_signature not in storage:
                    storage[method_signature] = {}
                    storage[method_signature]["tokens"] = []
                    storage[method_signature]["values"] = collections.Counter()

                for value in values:
                    if not is_value_to_filter(value):
                        storage[method_signature]["values"][value] += 1

    if not os.path.exists(args.output):
        os.makedirs(args.output)

    logger().info('Processing tuples -- START')
    tuples = process_chunks(args.input, keep_tuple, process_tuple)
    logger().info('Processing tuples -- DONE')

    tuples_path = os.path.join(args.output, 'tuples.json')
    with open(tuples_path, "w+") as tf:
        json.dump(tuples, tf)


if __name__ == '__main__':
    main()
