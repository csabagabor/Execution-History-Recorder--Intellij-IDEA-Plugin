
package gabor.history.debug;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.SuspendManager;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl;
import com.sun.jdi.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import gabor.history.debug.type.ComplexType;
import gabor.history.debug.type.PlainType;
import gabor.history.debug.type.var.*;
import gabor.history.debug.view.helper.NoMethodException;
import gabor.history.helper.BreakpointHelper;
import gabor.history.helper.LoggingHelper;
import reuse.sequence.Constants;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.ClassDescription;
import reuse.sequence.generator.MethodDescription;

import java.util.*;

public class DebugExtractor implements DebuggerCommand {
    private static final Set<String> BOXED_TYPES = new HashSet<>();
    private static final Set<String> COLLECTION_TYPES = new HashSet<>();
    private static final Set<String> MAP_TYPES = new HashSet<>();
    private int maxFieldRecursiveLimit = 2;
    public static final int STACK_SIZE_TO_IGNORE = 4;//Redirector
    public static final long TIMEOUT_VARAIBLE_EXTRACT_MILLISECONDS = 100L;
    private final StackFrameProxyImpl frameProxy;
    private final DebugProcessImpl debugProcess;
    private final SuspendContextImpl suspendContext;
    private final CallStack callStack;
    private final Set<String> projectClasses;
    private final CallStack prevBreakpointCallStack;
    private int nrCollectionItems = 5;
    private int nrFieldsToShow = 10;
    private boolean includeCollections = true;

    static {
        BOXED_TYPES.add("Boolean");
        BOXED_TYPES.add("Integer");
        BOXED_TYPES.add("Long");
        BOXED_TYPES.add("Float");
        BOXED_TYPES.add("Double");
        BOXED_TYPES.add("Short");
        BOXED_TYPES.add("Character");
        BOXED_TYPES.add("Byte");


        COLLECTION_TYPES.add("List");
        COLLECTION_TYPES.add("Set");
        COLLECTION_TYPES.add("Vector");
        COLLECTION_TYPES.add("Queue");
        COLLECTION_TYPES.add("Deque");


        MAP_TYPES.add("Map");
        MAP_TYPES.add("Hashtable");
    }

    public DebugExtractor(StackFrameProxyImpl frameProxy,
                          SuspendContextImpl suspendContext,
                          CallStack callStack,
                          Set<String> projectClasses,
                          CallStack prevBreakpointCallStack,
                          int maxFieldRecursiveLimit,
                          boolean includeCollections,
                          int nrCollectionItems,
                          int nrFieldsToShow) {
        this.frameProxy = frameProxy;
        this.debugProcess = suspendContext.getDebugProcess();
        this.suspendContext = suspendContext;
        this.callStack = callStack;
        this.projectClasses = projectClasses;
        this.prevBreakpointCallStack = prevBreakpointCallStack;
        if (nrCollectionItems >= 1) {
            this.nrCollectionItems = nrCollectionItems;
        }
        if (nrFieldsToShow >= 1) {
            this.nrFieldsToShow = nrFieldsToShow;
        }
        if (maxFieldRecursiveLimit >= 1) {
            this.maxFieldRecursiveLimit = maxFieldRecursiveLimit - 1;
        }

        this.includeCollections = includeCollections;
    }

    private CallStack getCallStackFromStackFrame(StackFrame frame, int index, boolean isFirst) {
        Location location = frame.location();
        Method method = location.method();
        ReferenceType referenceType = location.declaringType();

        if (referenceType != null) {
            ClassDescription classDescription = new ClassDescription(referenceType.name());

            MethodDescription methodDescription;
            if ("<init>".equals(method.name())) {
                methodDescription = new MethodDescription(classDescription, new ArrayList<>(), Constants.CONSTRUCTOR_METHOD_NAME,
                        method.returnTypeName(),
                        new ArrayList<>(), new ArrayList<>(method.argumentTypeNames()));
            } else {

                methodDescription = new MethodDescription(classDescription, new ArrayList<>(), method.name(),
                        method.returnTypeName(),
                        new ArrayList<>(), new ArrayList<>(method.argumentTypeNames()));
            }
            methodDescription.setLine(location.lineNumber());

            CallStack callStack = new CallStack(methodDescription);
            callStack.setIndex(index);

            if (projectClasses != null && !projectClasses.contains(referenceType.name())) {
                callStack.setProjectClass(false);
            }

            //if first call stack then set it to the hashcode of the original call
            if (isFirst) {
                methodDescription.setHashOfCallStack(this.callStack.hashCode());
            } else {
                methodDescription.setHashOfCallStack(callStack.hashCode());
            }
            return callStack;
        }
        return null;
    }

    @Override
    public void action() {
        ThreadReferenceProxyImpl myThreadProxy = frameProxy.threadProxy();
        ThreadReference threadRef = myThreadProxy.getThreadReference();

        //disable all types of requests, they can cause deadlock
        List<EventRequest> requests = new ArrayList<>();
        try {
            EventRequestManager manager = threadRef.virtualMachine().eventRequestManager();

            manager.breakpointRequests().forEach(er -> disableRequest(er, requests));
            manager.exceptionRequests().forEach(er -> disableRequest(er, requests));

            manager.classUnloadRequests().forEach(er -> disableRequest(er, requests));
            manager.classPrepareRequests().forEach(er -> disableRequest(er, requests));

            manager.methodEntryRequests().forEach(er -> disableRequest(er, requests));
            manager.methodExitRequests().forEach(er -> disableRequest(er, requests));

            manager.accessWatchpointRequests().forEach(er -> disableRequest(er, requests));
            manager.modificationWatchpointRequests().forEach(er -> disableRequest(er, requests));

            manager.threadStartRequests().forEach(er -> disableRequest(er, requests));
            manager.threadDeathRequests().forEach(er -> disableRequest(er, requests));

            manager.monitorContendedEnteredRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorContendedEnterRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorWaitedRequests().forEach(er -> disableRequest(er, requests));
            manager.monitorWaitRequests().forEach(er -> disableRequest(er, requests));

            manager.vmDeathRequests().forEach(er -> disableRequest(er, requests));

            manager.stepRequests().forEach(er -> disableRequest(er, requests));
        } catch (Throwable e) {

        }

        try {
            StackFrame frame = frameProxy.getStackFrame();
            ObjectReference thisObject = frame.thisObject();

            if (thisObject != null) {
                // if the frame is in an object
                ReferenceType referenceType = thisObject.referenceType();
                if (BreakpointHelper.CLASS_DATA_FULL_NAME.equals(referenceType.name())) {

                    try {
                        List<LocalVariable> localVariables = frame.visibleVariables();
                        Map<LocalVariable, Value> map = frame.getValues(localVariables);
//                        updateCache(map);

                        Optional<Map.Entry<LocalVariable, Value>> hitsMap =
                                map.entrySet().stream().filter(entry -> "hitsMap".equals(entry.getKey().name())).findFirst();

                        createCallStack(myThreadProxy, threadRef, hitsMap);
                    } catch (Throwable e) {
                        LoggingHelper.error(e);
                    }

                    //resume requests
                    for (EventRequest request : requests) {
                        request.enable();
                    }

                    resumeIfOnlyRequestor();
                }

            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    private void disableRequest(EventRequest request, List<EventRequest> disabled) {
        request.disable();
        disabled.add(request);
    }

    private void createCallStack(ThreadReferenceProxyImpl myThreadProxy, ThreadReference threadRef,
                                 Optional<Map.Entry<LocalVariable, Value>> hitsMap) {

        if (hitsMap.isPresent()) {
            Map<String, Integer> hitsConstructedMap = getMapFromMapValue(hitsMap.get().getValue());

            try {
                int index = myThreadProxy.frameCount() - 1;
                StackFrame frame;

                //find first project class called
                for (; index >= frameProxy.getFrameIndex() + STACK_SIZE_TO_IGNORE; index--) {//+STACK_SIZE_TO_IGNORE because of agent stack
                    frame = getStackFrameByIndex(myThreadProxy, threadRef, index);
                    CallStack firstCallStack = getCallStackFromStackFrame(frame, index, true);

                    if (firstCallStack != null) {
                        String className = firstCallStack.getMethod().getClassDescription().getClassName();

                        //do not ignore inner classes
                        int indexOfDollarSign = className.indexOf("$");
                        if (indexOfDollarSign >= 0) {
                            className = className.substring(0, indexOfDollarSign);
                        }
                        if (!projectClasses.isEmpty() && projectClasses.contains(className)) {//check if not empty to be sure that it was populated => else empty call stack
                            Location location = frame.location();

                            int lineNumber = -1;
                            if (location != null) {
                                lineNumber = location.lineNumber();
                            }
                            String positionInClassName = className + "#" + lineNumber;
                            Integer hits = hitsConstructedMap.get(positionInClassName);

                            if (hits != null) {
                                callStack.setHits(hits);//Java is pass-by-value, will lose callStack reference
                            }

                            callStack.setIndex(firstCallStack.getIndex());
                            callStack.setMethod(firstCallStack.getMethod());//Java is pass-by-value, will lose callStack reference
                            break;
                        }
                    }
                }

                //can be ignored because only a project class is ever instrumented(?) so it will be the last in the callstack(just before ClassData)
                //find last project class called
//                int lastProjectClassIndex = index;
//                for (int i = index - 1; i >= frameProxy.getFrameIndex() + STACK_SIZE_TO_IGNORE; i--) {//+STACK_SIZE_TO_IGNORE because of agent stack
//                    frame = getStackFrameByIndex(myThreadProxy, threadRef, i);
//                    CallStack callStack = getCallStackFromStackFrame(frame, i, false);
//
//                    if (callStack != null) {
//                        String className = callStack.getMethod().getClassDescription().getClassName();
//
//                        if (!projectClasses.isEmpty() && projectClasses.contains(className)) {
//                            lastProjectClassIndex = i;
//                        }
//                    }
//                }

                int lastProjectClassIndex = frameProxy.getFrameIndex() + STACK_SIZE_TO_IGNORE;

                CallStack prevCallStack = callStack;
                for (int i = index - 1; i >= lastProjectClassIndex; i--) {//+STACK_SIZE_TO_IGNORE because of agent stack
                    frame = getStackFrameByIndex(myThreadProxy, threadRef, i);

                    CallStack subCallStack = getCallStackFromStackFrame(frame, i, false);

                    if (subCallStack != null) {
                        //set hits
                        String className = subCallStack.getMethod().getClassDescription().getClassName();
                        Location location = frame.location();

                        int lineNumber = -1;
                        if (location != null) {
                            lineNumber = location.lineNumber();
                        }
                        String positionInClassName = className + "#" + lineNumber;
                        Integer hits = hitsConstructedMap.get(positionInClassName);

                        if (hits != null) {
                            subCallStack.setHits(hits);
                        }

                        subCallStack.setParent(prevCallStack);
                        prevCallStack.setCalls(new ArrayList<>(Collections.singletonList(subCallStack)));
                        prevCallStack = subCallStack;
                    }
                }

                //callstack is ready, compare it to previous callstack
                CallStack currentCall = callStack;
                CallStack prevCall = prevBreakpointCallStack;

                //find last callstack which is common between the two
                boolean foundCommon = false;
                try {
                    while (prevCall != null && prevCall.getMethod() != null &&
                            currentCall != null && currentCall.getMethod() != null) {
                        if (prevCall.getMethod().equals(currentCall.getMethod())
                                && prevCall.getHits() == currentCall.getHits()) {


                            CallStack parent = currentCall.getParent();
                            CallStack prevBreakpointParent = prevCall.getParent();

                            if (parent == null ||
                                    prevBreakpointParent == null ||
                                    parent.getMethod().getLine() == prevBreakpointParent.getMethod().getLine()) {//method cannot be null, checked before

                                List<CallStack> calls = prevCall.getCalls();

                                if (calls.size() > 0 && calls.get(calls.size() - 1) != null) {
                                    List<CallStack> currentCalls = currentCall.getCalls();

                                    if (currentCalls.size() > 0 && currentCalls.get(currentCalls.size() - 1) != null) {
                                        foundCommon = true;
                                        //only advance the two callstacks if both are not null
                                        //prevCall
                                        calls.get(calls.size() - 1).setRealParent(prevCall);//else we'll lose it
                                        List<HistoryVar> variables = prevCall.getVariables();
                                        prevCall = calls.get(calls.size() - 1);
                                        currentCall.setVariables(variables);//variables are not extracted multiple times for the same method if the stack frames have common elements

                                        //currentCall
                                        currentCall = currentCalls.get(currentCalls.size() - 1);
                                    } else {
                                        break;
                                    }

                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }


                    if (foundCommon && prevCall != null && currentCall != null) {
                        CallStack parent = prevCall.getRealParent();
                        List<CallStack> calls = parent.getCalls();
                        calls.add(currentCall);

                        callStack.setCanRemove(true);
                    }

                    //extract local variables at the end to prevent stateExceptions
                    //also start from the parent of currentCall because variables may change between 2 method calls
                    //e.g
                    //public void a(){
                    //  f1();//variables were extracted here
                    //  int var = 5;
                    //  f2();//but now we are here and 'var' was not included when variables were extracted at f1()
                    //}
                    CallStack tempStack = currentCall == null || currentCall.getParent() == null ? currentCall : currentCall.getParent();

                    try {
                        while (tempStack != null) {
                            //must be called in this order
                            //******************************************************
                            tempStack.setVariables(new ArrayList<>());
                            //******************************************************
                            extractLocalVariables(tempStack, getStackFrameByIndex(myThreadProxy, threadRef, tempStack.getIndex()));
                            extractFields(tempStack, getStackFrameByIndex(myThreadProxy, threadRef, tempStack.getIndex()));

                            List<CallStack> calls = tempStack.getCalls();
                            if (calls != null && calls.size() > 0) {
                                tempStack = calls.get(0);
                            } else {
                                tempStack = null;
                            }
                        }
                    } catch (Throwable e) {
                        //com.github.csabagabor.helper.LoggingHelper.error(e);
                    }
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }

            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }
    }

    private void extractLocalVariables(CallStack tempStack, StackFrame frame) {
        //extract variables only for callstacks which are not duplicate(variables for duplicate callstacks
        //been already extracted during the previous breakpoint
        try {
            if (frame == null) {
                return;
            }

            List<LocalVariable> localVariables = frame.visibleVariables();
            Map<LocalVariable, Value> map = frame.getValues(localVariables);

            long startTime = System.currentTimeMillis();
            long endTime = startTime + TIMEOUT_VARAIBLE_EXTRACT_MILLISECONDS;//prevent long running extractions
            map.forEach(((var, value) -> {
                try {
                    if (System.currentTimeMillis() < endTime) {
                        String name = var.name();
                        if (!"__class__data__".equals(name)) {

                            HistoryVar historyVariable = getVariableBasedOnType(name, value);
                            //null check in addVariablesRecursively in case value is null
                            addVariablesRecursively(value, historyVariable, 0, endTime);
                            tempStack.getVariables().add(historyVariable);
                        }
                    }
                } catch (Throwable e) {
                    LoggingHelper.debug(e);
                }
            }));
        } catch (
                Throwable e) {
            //com.github.csabagabor.helper.LoggingHelper.error(e);
        }

    }

    private HistoryVar getVariableBasedOnType(String name, Value value) {
        if (value == null) {
            return new HistoryLocalVariable(name, "null");
        }
        if (value instanceof StringReference) {
            return new HistoryLocalVariable(name, value.toString());
        } else if (value instanceof ArrayReference) {
            ObjectReference oref = (ArrayReference) value;
            return new HistoryArrayVariable(name, new ComplexType(oref.referenceType().name(), oref.uniqueID()));
        } else if (value instanceof ObjectReference) {
            ObjectReference oref = (ObjectReference) value;

            //Map
            String refName = oref.referenceType().name();
            if (refName != null && refName.startsWith("java.util") && (refName.contains("Map") || refName.contains("Hashtable")) && refName.contains("$")) {
                Value mapKey = invokeMethodOnObject(oref, "getKey");
                Value mapValue = invokeMethodOnObject(oref, "getValue");
                return new HistoryEntryVariable(mapKey != null ? mapKey.toString() : "", new PlainType(mapValue != null ? mapValue.toString() : ""));
            }

            Type type = oref.type();
            if (type instanceof ClassType) {
                boolean isEnum = ((ClassType) type).isEnum();
                if (isEnum) {
                    return new HistoryEnumVariable(name, new ComplexType(oref.referenceType().name(), oref.uniqueID()));
                }
            }

            return new HistoryLocalVariable(name, new ComplexType(oref.referenceType().name(), oref.uniqueID()));
        } else if (value instanceof PrimitiveValue) {
            return new HistoryPrimitiveVariable(name, new PlainType(value.toString()));
        }

        return new HistoryLocalVariable(name, value.toString());
    }

    /*
    return value used to indicate that max depth is reached
     */
    private boolean addVariablesRecursively(Value value, HistoryVar historyVariable, int limit, long endTime) {
        if (value == null) {//added for safety, it won't enter in any of the if blocks
            return true;
        }
        if (System.currentTimeMillis() > endTime) {
            return true;
        }

        if (limit >= maxFieldRecursiveLimit + 1) {
            return false;
        }

        List<HistoryVar> fieldVariables = new ArrayList<>();
        historyVariable.setFieldVariables(fieldVariables);
        try {
            if (value instanceof PrimitiveValue || value instanceof StringReference) {
                return true;
            } else if (value instanceof ArrayReference) {
                Integer size = extractArrayReference((ArrayReference) value, limit, endTime, fieldVariables);
                //only applies to collections, maps, arrays show size next to variable
                if (size != null) {
                    historyVariable.setSize(size);
                }
            } else if (value instanceof ObjectReference) {
                ObjectReference objectReference = (ObjectReference) value;
                ReferenceType referenceType = objectReference.referenceType();
                final String refName = referenceType.name();
                final String className;
                final String packageName;
                int lastIndexOfDot = refName.lastIndexOf(".");
                if (lastIndexOfDot >= 0) {
                    className = refName.substring(lastIndexOfDot + 1);
                    packageName = refName.substring(0, lastIndexOfDot);
                } else {
                    packageName = "";
                    className = refName;
                }

                //if we show primitives then show boxed types as well
                if (packageName.equals("java.lang") && BOXED_TYPES.contains(className)) {
                    fieldVariables.add(new HistoryLocalVariable("value",
                            new PlainType(invokeToString(objectReference))));
                    return true;
                }

                //ignore if other types
                if (limit >= maxFieldRecursiveLimit) {
                    return false;
                }

                Type type = objectReference.type();
                ClassType classType = null;
                if (type instanceof ClassType) {
                    classType = (ClassType) type;

                    //best approach to check if it's a collection or Map would be to call Collection.class.isAssignableFrom(classType) but this needs an `invokeMethod` which is slow
                    boolean isFromJavaUtils = packageName.startsWith("java.util");//startsWith because java.util.concurrent etc.
                    if (includeCollections && COLLECTION_TYPES.stream().anyMatch(className::contains) && (isFromJavaUtils || isCollection(classType))) {//first check name(this won't be 100% accurate but at least it's fast)
                        ArrayReference arrayReference = getValueFromCollection(value);
                        Integer size = extractArrayReference(arrayReference, limit, endTime, fieldVariables);

                        //only applies to collections, maps, arrays show size next to variable
                        if (size != null) {
                            historyVariable.setSize(size);
                        }
                    } else if (includeCollections && MAP_TYPES.stream().anyMatch(className::contains) && isFromJavaUtils) {//todo make isMap()  to work
                        if (isFromJavaUtils && className.contains("$")) {

                            Value mapKey = invokeMethodOnObject(objectReference, "getKey");
                            Value mapValue = invokeMethodOnObject(objectReference, "getValue");

                            HistoryVar var1 = getVariableBasedOnType("key", mapKey);
                            fieldVariables.add(var1);
                            addVariablesRecursively(mapKey, var1, limit + 1, endTime);

                            HistoryVar var2 = getVariableBasedOnType("value", mapValue);
                            fieldVariables.add(var2);
                            addVariablesRecursively(mapValue, var2, limit + 1, endTime);
                        } else {
                            ArrayReference arrayReference = getValueFromMap(value);
                            Integer size = extractArrayReference(arrayReference, limit, endTime, fieldVariables);

                            //only applies to collections, maps, arrays show size next to variable
                            if (size != null) {
                                historyVariable.setSize(size);
                            }
                        }
                    } else if (classType.isEnum()) {
                        HistoryVar var1 = getVariableBasedOnType("name", invokeMethodOnObject(objectReference, "toString"));
                        fieldVariables.add(var1);

                        HistoryVar var2 = getVariableBasedOnType("ordinal", invokeMethodOnObject(objectReference, "ordinal"));
                        fieldVariables.add(var2);
                    } else if (projectClasses.contains(refName)) {//other type from project
                        List<Field> fieldList = referenceType.visibleFields();
                        Map<Field, Value> fieldMap = objectReference.getValues(fieldList);

                        int pc = 0;
                        for (Map.Entry<Field, Value> entry : fieldMap.entrySet()) {
                            try {
                                pc++;
                                if (pc > nrFieldsToShow) {
                                    fieldVariables.add(new HistoryEntryVariable("...", new PlainType("...(change settings to include more fields)")));
                                    return false;
                                }

                                Field field = entry.getKey();
                                Value fieldValue = entry.getValue();
                                //no need to check if fieldValue is not null
                                HistoryVar fieldVariable = getVariableBasedOnType(field.name(), fieldValue);

                                //recursively get fieldvariables until limit
                                addVariablesRecursively(fieldValue, fieldVariable, limit + 1, endTime);

                                fieldVariables.add(fieldVariable);
                            } catch (Throwable e) {
                                LoggingHelper.debug(e);
                            }
                        }
                    } else {
                        //no need for this
//                        fieldVariables.add(new HistoryLocalVariable(referenceType.name(),
//                                new PlainType(String.valueOf(objectReference.hashCode()))));//invokeToString() was used initially but can slow down execution
                    }
                }
            }
        } catch (
                NoMethodException e) {
            LoggingHelper.debug(e);
        } catch (
                Throwable e) {
            LoggingHelper.error(e);
        }
        return true;
    }

    private boolean isMap(ClassType classType) {
        List<InterfaceType> interfaces = classType.interfaces();//cached method under the hood
        Optional<InterfaceType> isCollection = interfaces.stream().limit(5).filter(i -> i != null && i.name() != null && i.name().startsWith("java.util") && i.name().contains("Map")).findAny();

        return isCollection.isPresent();
    }

    private boolean isCollection(ClassType classType) {
        List<InterfaceType> interfaces = classType.interfaces();//cached method under the hood
        Optional<InterfaceType> isCollection = interfaces.stream().limit(5).filter(
                i -> i != null && i.name() != null &&
                        (i.name().startsWith("java.util") && COLLECTION_TYPES.stream().anyMatch(type -> i.name().contains(type)))
        ).findAny();

        return isCollection.isPresent();
    }

    private Integer extractArrayReference(ArrayReference value, int limit, long endTime, List<HistoryVar> fieldVariables) {
        if (value != null) {
            List<Value> values = value.getValues();
            if (values != null) {
                for (int i = 0; i < values.size(); i++) {
                    if (i == nrCollectionItems) {
                        fieldVariables.add(new HistoryEntryVariable("...", new PlainType("...(change settings to include more elements)")));
                        break;
                    }
                    HistoryVar historyLocalVariable = getVariableBasedOnType(String.valueOf(i), values.get(i));
                    fieldVariables.add(historyLocalVariable);

                    //recursively get fieldvariables until limit
                    addVariablesRecursively(values.get(i), historyLocalVariable, limit + 1, endTime);
                }
                return values.size();
            }
        }
        return null;
    }

    private Object toType(Value value) {
        if (value instanceof StringReference) {
            return value.toString();
        } else if (value instanceof ObjectReference) {
            ObjectReference oref = (ObjectReference) value;
            return new ComplexType(oref.referenceType().name(), oref.uniqueID());
        } else if (value instanceof PrimitiveValue) {
            return new PlainType(value.toString());
        }

        return null;
    }

    private StackFrame getStackFrameByIndex(ThreadReferenceProxyImpl myThreadProxy, ThreadReference threadRef, int index) {
        try {
            if (index > 0 && index < 10) {
                return threadRef.frames(0, Math.min(myThreadProxy.frameCount(), 10)).get(index);
            } else {
                return threadRef.frame(index);
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
            return null;
        }
    }

    private void resumeIfOnlyRequestor() {
        SuspendManager suspendManager = debugProcess.getSuspendManager();
        suspendManager.resume(suspendContext);
    }

    private void extractFields(CallStack tempStack, StackFrame frame) {
        try {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + TIMEOUT_VARAIBLE_EXTRACT_MILLISECONDS;//prevent long running extractions

            if (frame == null) {
                return;
            }

            ObjectReference thisObject = frame.thisObject();
            List<HistoryVar> fieldVariables = new ArrayList<>();

            HistoryLocalVariable historyVariable;
            ReferenceType referenceType;
            if (thisObject != null) {

                // if the frame is in an object
                referenceType = thisObject.referenceType();

                String name = referenceType.name();
                if ("modified.com.intellij.rt.coverage.data.ClassData".equals(name)) {
                    return;
                }
                historyVariable = new HistoryLocalVariable("this",
                        toType(thisObject));

                List<Field> fieldList = referenceType.visibleFields();
                Map<Field, Value> fieldMap = thisObject.getValues(fieldList);

                int pc = 0;
                for (Map.Entry<Field, Value> entry : fieldMap.entrySet()) {
                    try {
                        pc++;
                        if (pc > nrFieldsToShow) {
                            fieldVariables.add(new HistoryEntryVariable("...", new PlainType("...(change settings to include more fields)")));
                            return;
                        }
                        Field field = entry.getKey();
                        Value value = entry.getValue();

                        //no need to check if value is null, null check is added to the following methods
                        HistoryVar fieldVariable = getVariableBasedOnType(field.name(), value);
                        addVariablesRecursively(value, fieldVariable, 0, endTime);
                        fieldVariables.add(fieldVariable);
                    } catch (Throwable e) {
                        LoggingHelper.debug(e);
                    }
                }
            } else {  // if the frame is in a native or static method
                referenceType = frame.location().declaringType();

                String name = referenceType.name();
                if ("modified.com.intellij.rt.coverage.data.ClassData".equals(name)) {
                    return;
                }

                historyVariable = new HistoryLocalVariable("static", new ComplexType(name, referenceType.hashCode()));

                List<Field> fields = referenceType.fields();
                for (int i = 0; i < fields.size(); i++) {
                    if (i > nrFieldsToShow) {
                        fieldVariables.add(new HistoryEntryVariable("...", new PlainType("...(change settings to include more fields)")));
                        return;
                    }

                    Field field = fields.get(i);
                    if (field.isStatic()) {//else ava.lang.IllegalArgumentException: Attempt to use non-static field with ReferenceType is thrown
                        try {
                            Value value = referenceType.getValue(field);
                            //no need to check if value is null, null check is added to the following methods

                            HistoryVar fieldVariable = getVariableBasedOnType(field.name(), value);
                            addVariablesRecursively(value, fieldVariable, 0, endTime);
                            fieldVariables.add(fieldVariable);
                        } catch (Throwable e) {
                            LoggingHelper.debug(e);
                        }
                    }
                }
            }
            historyVariable.setFieldVariables(fieldVariables);

            tempStack.getVariables().add(historyVariable);
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    private Map<String, Integer> getMapFromMapValue(Value value) {

        if (!(value instanceof ObjectReference)) {
            return new HashMap<>();
        }

        Map<String, Integer> map = new HashMap<>();
        try {
            String toString = invokeToString((ObjectReference) value);
            toString = toString.replaceAll("[{]", "");
            toString = toString.replaceAll("[}]", "");
            toString = toString.replaceAll(" ", "");
            String[] allPairs = toString.split(",");
            for (String allPair : allPairs) {
                String[] splitPair = allPair.split("=");
                for (int i = 0; i < splitPair.length - 1; i += 2) {
                    String className = splitPair[i];
                    String hits = splitPair[i + 1];


                    map.put(className, Integer.valueOf(hits));
                }
            }

        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        return map;
    }

    private ArrayReference getValueFromMap(Value value) {
        if (!(value instanceof ObjectReference)) {
            return null;
        }

        try {
            Value entrySet = invokeMethodOnObject((ObjectReference) value, "entrySet");
            ArrayReference arrayReference = (ArrayReference) invokeMethodOnObject((ObjectReference) entrySet, "toArray");
            return arrayReference;
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        return null;
    }


    private ArrayReference getValueFromCollection(Value value) {
        if (!(value instanceof ObjectReference)) {
            return null;
        }

        try {
            ArrayReference arrayReference = (ArrayReference) invokeMethodOnObject((ObjectReference) value, "toArray");
            return arrayReference;
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        return null;
    }

    private Value invokeMethodOnObject(ObjectReference object, String methodName) {
        try {
            if (object == null) {//scratchFile breakpoint cannot execute this for some reason
                return null;
            }

            ReferenceType referenceType = object.referenceType();

            List<Method> methods = referenceType.methodsByName(methodName);

            Method method = null;
            for (Method m : methods) {
                if (m.argumentTypeNames().size() == 0) {
                    method = m;
                    break;
                }
            }

            if (method == null) {
                throw new NoMethodException(referenceType.name() + "#" + methodName);
            }

            ThreadReference threadRef = frameProxy.threadProxy().getThreadReference();

            Value value = object.invokeMethod(threadRef, method,
                    Collections.EMPTY_LIST, ObjectReference.INVOKE_SINGLE_THREADED);

            return value;
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
        return null;
    }

    private String invokeToString(ObjectReference object) {
        if (object == null) {
            return "";
        }
        return trimQuotes(invokeMethodOnObject(object, "toString").toString());
    }

    private String trimQuotes(String string) {
        if (string.length() >= 2 && string.charAt(0) == '"' && string.charAt(string.length() - 1) == '"') {
            string = string.substring(1, string.length() - 1);
        }
        return string;
    }

    @Override
    public void commandCancelled() {

    }
}