package com.weather.airlock.sdk.util;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.exceptions.AirlockInvalidFileException;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * @author Denis Voloshin on 05/11/2017.
 */

public class FileUtil {

    public static String readAndroidFile(int fileId, Context context) throws AirlockInvalidFileException, IOException {
        if (fileId == Constants.INVALID_FILE_ID) {
            throw new AirlockInvalidFileException(AirlockMessages.ERROR_INVALID_FILE_ID);
        }

        InputStream inStream = context.openRawResource(fileId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        while ((strLine = br.readLine()) != null) {
            sBuilder.append(strLine);
        }
        inStream.reset();
        br.close();
        return sBuilder.toString();
    }

    static public String readStringFromResource(android.content.Context context, int resourceID) {
        StringBuilder contents = new StringBuilder();
        String sep = System.getProperty("line.separator");

        try {
            InputStream is = context.getResources().openRawResource(resourceID);

            BufferedReader input =  new BufferedReader(new InputStreamReader(is), 1024*8);
            try {
                String line = null;
                while (( line = input.readLine()) != null){
                    contents.append(line);
                    contents.append(sep);
                }
            }
            finally {
                input.close();
            }
        }
        catch (FileNotFoundException ex) {
            return "{}";
        }
        catch (IOException ex){
            return "{}";
        }

        return contents.toString();
    }
}
