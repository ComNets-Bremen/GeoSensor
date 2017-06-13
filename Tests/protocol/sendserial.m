function send = sendserial(string)
    delete(instrfindall);
    serialport = serial('/dev/tty.SLAB_USBtoUART','baudrate',38400);
    serialport.OutputBufferSize = 128000000;
    serialport.Terminator = '';
    serialport.Timeout = 1000;
    fopen(serialport)
    fwrite(serialport,3);
    pause(1)
    fwrite(serialport,char(string))
    fwrite(serialport,3);
    fclose(serialport);
    send = 1; 
end

