package itis.parsing2;

import itis.parsing2.annotations.Concatenate;
import itis.parsing2.annotations.NotBlank;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactoryParsingServiceImpl implements FactoryParsingService {

    @Override
    public Factory parseFactoryData(String factoryDataDirectoryPath) throws FactoryParsingException {
        System.out.println("22");
        List<String> fileLines = new ArrayList<>();
        //write your code here
        Class<Factory> factoryClass=Factory.class;

        Constructor<Factory> constructor=null;
        try {
            constructor=factoryClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        constructor.setAccessible(true);
        Factory factory= null;

        try {
            factory=constructor.newInstance();

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        File file= new File(factoryDataDirectoryPath);
       File[] files= file.listFiles();

        Map<String,String> parametrs= new HashMap<>();
        Map<String,List<String>> departmentsParam= new HashMap<>();

        for (File file1:files) {
            try {
                fileLines = Files.readAllLines(Paths.get(file1.getAbsolutePath()));
            } catch (IOException e) {
                e.printStackTrace();
            }



            for (String line:fileLines) {

                System.out.println("33");
                if (!line.equals("---")) {
                    String value="";

                    String[] parts = line.split(":");
                    String key=parts[0].replaceAll("\"","").trim();

                    if (key.equals("departments")){
                        List<String> departments=new ArrayList<>();
                        char[] values=parts[1].toCharArray();
                        String word="";
                        int f=0;
                            while (f!= values.length-1) {
                                System.out.println("44");
                                if (values[f] == '\"') {
                                    int k=f+1;
                                    while (values[k] != '\"') {

                                        word = word + values[k];
                                        k++;
                                    }

                                    departments.add(word);
                                    word = "";
                                    f = k+1;

                                } else {
                                    f=f+1;
                                }
                            }


                        departmentsParam.put(key,departments);

                    } else {
                         value=parts[1].replaceAll("\"","").trim();
                    }

                     if (key.equals("title")){
                         parametrs.put(key,value);

                     } else if (key.equals("organizationChiefFullName")){

                         parametrs.put(key,value);
                     } else if (key.equals("description")){

                         parametrs.put(key,value);

                     } else if (key.equals("amountOfWorkers")){

                         parametrs.put(key,value);

                     }

                }



            }




        }



        return fieldWorker(factory,factoryClass,parametrs,departmentsParam);
    }


    Factory fieldWorker(Factory factory1,Class factoryClass,Map<String,String> parametrs,Map<String,List<String>> departmentsParam){

        Constructor<Factory> constructor=null;
        try {
            constructor=factoryClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        constructor.setAccessible(true);
        Factory factory= null;

        try {
            factory=constructor.newInstance();

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Field[] fields=factoryClass.getDeclaredFields();
        List<FactoryParsingException.FactoryValidationError> errors= new ArrayList<>();
        for (Field field:fields) {
            field.setAccessible(true);
            NotBlank notBlank=field.getDeclaredAnnotation(NotBlank.class);
            Concatenate concatenate= field.getDeclaredAnnotation(Concatenate.class);
           boolean flagError=false;
            if (notBlank!=null){
              String strValue=  field.getName();
              if (strValue.equals("departments")){
                List<String> value=  departmentsParam.get(strValue);
                  if (value==null){

                      errors.add(new FactoryParsingException.FactoryValidationError(field.getName(),"Поле пустое!"));

                  }
              } else{
                  String value=parametrs.get(strValue);
                  if (value.equals("")||value.equals("null")){

                      errors.add(new FactoryParsingException.FactoryValidationError(field.getName(),"Поле пустое!"));
                        flagError=true;

                  }
              }

            }
            if (concatenate!=null){
                if (concatenate.delimiter()==null||concatenate.fieldNames()==null){
                    errors.add(new FactoryParsingException.FactoryValidationError(field.getName(),"Не удалось провести конкатенацию!"));
                    flagError=true;
                } else {
                    String answer="";
                    for (String word:concatenate.fieldNames()) {
                       answer= answer.concat(concatenate.delimiter());
                        answer=answer.concat(word);

                    }
                    parametrs.remove(field.getName());
                    parametrs.put(field.getName(),answer);
                }


            }

            if (!flagError){
                try {
                  String strValue=  parametrs.get(field.getName());
                    field.set(factory,valueChanger(strValue,field.getType()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                try {
                    String strValue=  parametrs.get(field.getName());
                    field.set(factory,departmentsParam.get(strValue));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }
        boolean flag1=false;
        try {
            errorChecker(errors);
        }catch (FactoryParsingException e){
            System.out.println(e.getMessage());
            List<FactoryParsingException.FactoryValidationError> ValidationErrors=e.getValidationErrors();
            for (FactoryParsingException.FactoryValidationError validationError:ValidationErrors) {
                System.out.println(validationError.fieldName + " , "+ validationError.validationError);

            }
            flag1=true;
        } catch (Exception e){
            e.printStackTrace();
        }
        if (!flag1){
            return factory;
        } else {
            return null;
        }

    }

    Object valueChanger(String strValue, Class c){
        Long answer;
        if (c==Long.class){
            if (strValue==null||strValue.equals("")||strValue.equals("null")){
                answer=null;
            } else {
                 answer=Long.parseLong(strValue);
                return answer;
            }

        }
        return strValue;
    }

    void errorChecker(List<FactoryParsingException.FactoryValidationError> errors){
        if (errors.size()>0){

            throw  new FactoryParsingException("Ошибка парсинга", errors);
        }

    }

}
