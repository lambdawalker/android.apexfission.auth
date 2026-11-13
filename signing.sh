#!/bin/sh
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd) || exit 1
exec sh "$script_dir/setup.sh" signing "$@"
