import subprocess as sp
from time import sleep

import psutil


class DataReader:
    def __init__(self):
        self.NVIDA_COMMAND = "nvidia-smi --query-gpu=memory.used --format=csv"
        self.is_nvidia = True
        try:
            sp.check_output('nvidia-smi')
            self.is_nvidia = True
        except Exception:
            self.is_nvidia = False

    def get_pc_data(self):
        return {
            'cpu': psutil.cpu_percent(),
            'ram': psutil.virtual_memory().percent,
            'temperature': 12,
            'gpu': self.__get_gpu_memory()
        }

    def __get_gpu_memory(self):
        if self.is_nvidia:
            return self.__get_nvidia_gpu_memory()
        else:
            return self.__get_amd_gpu_memory()

    def __get_nvidia_gpu_memory(self):
        def output_to_list(x: bytes):
            return x.decode('ascii').split('\n')[:-1]
        try:
            memory_use_info = output_to_list(
                sp.check_output(self.NVIDA_COMMAND.split(), stderr=sp.STDOUT))[1:]
        except sp.CalledProcessError as e:
            raise RuntimeError("command '{}' return with error (code {}): {}".format(
                e.cmd, e.returncode, e.output))

        memory_use_values = [int(x.split()[0])
                             for i, x in enumerate(memory_use_info)]
        return memory_use_values[0]

    def __get_amd_gpu_memory(self):
        from pyadl import ADLManager

        device = ADLManager.getInstance().getDevices()[0]
        return device.getCurrentUsage()


while True:
    data_reader = DataReader()
    print(data_reader.get_pc_data())
    sleep(1)
