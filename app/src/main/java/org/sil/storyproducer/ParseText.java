package org.sil.storyproducer;

import android.content.Context;
import android.content.res.Resources;
import android.text.InputType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rjacubec on 10/9/2016.
 */

public class ParseText {
    private static boolean hasError = false;
    private static String errorString = "";
    private static String toReturn = "";
    private static final int TEXT = InputType.TYPE_CLASS_TEXT;
    private static final int EMAIL = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT;
    private static final int PHONE = InputType.TYPE_CLASS_PHONE;


    /***
     * This function serves to parse text for the registration field. This class may be agumented
     * to serve other activities that need input validation.
     * @param TextFieldType The type of text field that needs validation
     * @param inputtedTextField The actual text that will be validated.
     * @param classResources The resources of the activity so that the appropriate strings can be
     *                       accessed.
     * @return The string that was parsed or the error message.
     */
    public static String parseText(int TextFieldType, String inputtedTextField, Resources classResources){

        hasError = false;
        errorString = "";
        final String BYPASS_STRING =  classResources.getString(R.string.bypass_field_parse);
        Pattern parsingPattern;

        switch(TextFieldType){
            case TEXT:
                //do something with inputtedTextField
                if(inputtedTextField.equals("")){
                    String emptyError = classResources.getString(R.string.blank_text_field_error);
                    setError(emptyError);
                }else if(inputtedTextField.equals(BYPASS_STRING)){
                    //May have other logic to go in here
                    toReturn = inputtedTextField;
                }
                else{
                    toReturn = inputtedTextField;
                }
                break;
            case EMAIL:
                // do something with inputtedTextField
//                String regexString = classResources.getString(R.string.email_regex);
                if(inputtedTextField.equals(BYPASS_STRING)){
                    //May have other logic to go in here
                    toReturn = inputtedTextField;
                }
                else{
                    String regexString ="^[\\w\\p{Punct}&&[^.@]]+(([.]([\\w\\p{Punct}&&[^.@]]+))*)([@]){1}[\\w\\p{Punct}&&[^.@]]+([.][\\w\\p{Punct}&&[^.@]]+)*([.][A-Za-z]{3})$";
                    parsingPattern = Pattern.compile(regexString);
                    Matcher mc = parsingPattern.matcher(inputtedTextField);
                    if(!mc.find()){
                        String invalidEmailMsg = classResources.getString(R.string.email_invalid);
                        setError(String.format(invalidEmailMsg, inputtedTextField));
                    }else {
                        toReturn = inputtedTextField;
                    }
                }
                break;
            case PHONE:
                //do something with phone text field
                if(inputtedTextField.equals(BYPASS_STRING)){
                    //May have other logic to go in here
                    toReturn = inputtedTextField;
                }else{
                    toReturn = inputtedTextField;
                }
                break;
            default:
                setError("Something went wrong!");
                break;
        }

        return hasError ? errorString : toReturn;
    }

    public static boolean hasError(){
        return hasError;
    }

    private static void setError(String errorMsg){
        hasError = true;
        errorString = errorMsg;
    }
}
