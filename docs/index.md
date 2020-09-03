## Manual

1. Install it from the [Marketplace](https://plugins.jetbrains.com/plugin/14968-execution-history-recorder)     
>Note: If you have a large project go to step 4 before going to step 2. Also, I'd recommend trying out the plugin on some small project like this Spring Boot app: [https://github.com/spring-projects/spring-petclinic](https://github.com/spring-projects/spring-petclinic) 
2. Run any project and open the **Console Tab**. When you are ready to record, press the green recording button.   
> Note: If the button does not appear go to `Tools -> History Recorder -> Start History Recorder`  
![history](/images/start.bmp)  
3. Now you just have to wait until the button turns red. Also a notification will appear saying, recording is ON. **This will take a long time for a large project!!!!! In this case go to step 4**
4. Go to the Run configuration menu -> History Recorder Settings and include only the classes/packages you want to include. When including a package write `package1.package2.*`.  After this step you must restart the application.    
![history](/images/configuration.bmp)    
5. After recording is ON, you can carry out the actions you want in the app. Everything will be recorded until you press the (now) red recording button.  
6. Press the red button to stop recording.  
![history](/images/stop.bmp)  
7. Wait until the sequence diagram pops up. You can click on methods, arrows on the sequence diagram and it will lead you to the execution point.  
![history](/images/sequence.bmp)  
8. On the left hand side you can navigate the stack trace and on the right hand side you will be able to see variable values.  
![history](/images/execution.bmp)
9. Also, lines which have been executed are marked green.   
![history](/images/coverage.bmp)
10. Also, a button appears next to each method that is included in a stack trace. Press the button to load the stack trace containing that method.  
![history](/images/execution_points.bmp)
11. To delete all the recording information from the source files: icons, green/red lines etc. press `ALT + G` or go to `Tools -> History Recorder ->
 Remove History Recorder highlights`.
 12. Also you will be able to see variable information next to variables:  
![history](/images/hover.bmp)

## Export/Import Recording

1. Go to `Tools -> History Recorder ->  Open Recording from file`  
![history](/images/open_recording.bmp)
2. To save a recording (**only works if you haven't deleted the recording with ALT + G before**): go to `Tools -> History Recorder ->  Save Recording to file`.
3. You can send the exported file to anybody. It can be loaded on any OS, on any IntelliJ version. The same project must be opened.

## Run Configuration Settings:
> Note: all the 
1. Include patterns if your project is too large. By deafult, if you don't include any patterns, all the classes in your project will be included.  
![history](/images/patterns.bmp)
2. There are 3 modes in which you can run the agent:  
	- 1). Only coveraged lines: fastest option, shows only green/red lines
	- 2). Sequence Diagram + stack traces: without variable information, somewhat slower than 1st mode
	- 3). Variable information: slowest option. **Use it with caution: it might consume a lot of memory for large projects**
3. **Number of stack traces to show**: depending on memory consumption and execution time, you can set to a larger value.  
![history](/images/nr_breaks.bmp)
4. **Max number of occurrence of a single method** means how many number of times the same method is shown in the sequence diagram/stack traces. It does not make sense to set it to a high value, because it has a high impact on execution time(there can be methods which are called several thousand times...).
5. There are other options to limit the memory consumption like limit the number of collection items, number of fields to include, include getters/setters/constructors etc.

## Sequence Diagram filtering options:
1. **Filter classes:** only included stack frames which have the selected classes as their last execution points:  
![history](/images/filter.bmp)
2. **Search:** Searched classes and their methods will appear in red. 
![history](/images/search.bmp)
3. Export to png.
4. **Zoom in/out with mouse + scroll**.
5. Bird view in the right bottom corner.

## Issues and bugs
> Note: **only for debug purposes, not for general use:** There is a hack to enable and see debug logs in a JFrame. To enable it, just write `HISTORY_MAGIC` inside the exclude packages menu in the Run Configuration tab. A new Window will pop up. You can then open an issue on Github and attach the log messages. This will help me solve the problem... After you are done, remove `HISTORY_MAGIC` from the exclude list. 

## Future development
1. Show full variable information when hovering over variable (like in IntelliJ Debugger)
2. Change instrumented patterns/filters during runtime
3. Show accurate coverage percentages next to file names(currently it is calculated based on the total number of lines and not based on the lines containing executable code)
4. Kotlin support


### Support 
[Please check out my other plugins and give a review if you like them](https://plugins.jetbrains.com/author/b008256f-d5e7-4092-a142-ce7029345cec)
