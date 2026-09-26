#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running emulator.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample
for mode in light dark; do
  set_night_mode "$mode"
  for step in 0 1 2; do
    fresh_launch --ei tourStep "$step"
    capture "step$((step + 1))-$mode"
  done
done
