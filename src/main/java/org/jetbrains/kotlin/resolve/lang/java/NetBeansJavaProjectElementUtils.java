/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.resolve.lang.java;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.jetbrains.kotlin.projectsextensions.ClassPathExtender;
import org.jetbrains.kotlin.projectsextensions.KotlinProjectHelper;
import org.jetbrains.kotlin.resolve.lang.java.Searchers.BinaryNameSearcher;
import org.jetbrains.kotlin.resolve.lang.java.Searchers.IsDeprecatedSearcher;
import org.jetbrains.kotlin.resolve.lang.java.Searchers.PackageElementSearcher;
import org.jetbrains.kotlin.resolve.lang.java.Searchers.TypeElementSearcher;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Александр
 */
public class NetBeansJavaProjectElementUtils {
    
    private static final Map<Project,JavaSource> JAVA_SOURCE = new HashMap<Project,JavaSource>();
    private static final Map<Project,ClasspathInfo> CLASSPATH_INFO = new HashMap<Project,ClasspathInfo>();
    
    private static ClasspathInfo getClasspathInfo(Project kotlinProject){
        
        assert kotlinProject != null : "Project cannot be null";
        
        ClassPathExtender extendedProvider = KotlinProjectHelper.INSTANCE.getExtendedClassPath(kotlinProject);
        
        ClassPath boot = extendedProvider.getProjectSourcesClassPath(ClassPath.BOOT);
        ClassPath src = extendedProvider.getProjectSourcesClassPath(ClassPath.SOURCE);
        ClassPath compile = extendedProvider.getProjectSourcesClassPath(ClassPath.COMPILE);
        
        ClassPath bootProxy = ClassPathSupport.createProxyClassPath(boot, compile);
        
        return ClasspathInfo.create(bootProxy, src, compile);
    }
    
    public static Set<String> getPackages(Project project, String name) {
        if (!CLASSPATH_INFO.containsKey(project)){
            CLASSPATH_INFO.put(project, getClasspathInfo(project));
        }
        return CLASSPATH_INFO.get(project).getClassIndex().
                getPackageNames(name, false, EnumSet.of(ClassIndex.SearchScope.SOURCE, ClassIndex.SearchScope.DEPENDENCIES));
    }
    
    public static void updateClasspathInfo(Project kotlinProject){
        CLASSPATH_INFO.put(kotlinProject, getClasspathInfo(kotlinProject));
        JAVA_SOURCE.put(kotlinProject, JavaSource.create(CLASSPATH_INFO.get(kotlinProject)));
    }
    
    private static void checkJavaSource(Project kotlinProject) {
        if (!CLASSPATH_INFO.containsKey(kotlinProject)){
            CLASSPATH_INFO.put(kotlinProject, getClasspathInfo(kotlinProject));
        }
        if (!JAVA_SOURCE.containsKey(kotlinProject)){
            JAVA_SOURCE.put(kotlinProject,JavaSource.create(CLASSPATH_INFO.get(kotlinProject)));
        }
    }
    
    public static TypeElement findTypeElement(Project kotlinProject, String fqName){
        checkJavaSource(kotlinProject);
        TypeElementSearcher searcher = new TypeElementSearcher(fqName);
        try {
            JAVA_SOURCE.get(kotlinProject).runUserActionTask(searcher, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return searcher.getElement();
    }
    
    public static PackageElement findPackageElement(Project kotlinProject, String fqName){
        checkJavaSource(kotlinProject);
        PackageElementSearcher searcher = new PackageElementSearcher(fqName);
        try {
            JAVA_SOURCE.get(kotlinProject).runUserActionTask(searcher, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return searcher.getElement();
    }
    
    public static List<String> findFQName(Project kotlinProject, String name) {
        checkJavaSource(kotlinProject);
        List<String> fqNames = new ArrayList<String>();
        
        final Set<ElementHandle<TypeElement>> result = 
                CLASSPATH_INFO.get(kotlinProject).getClassIndex().
                        getDeclaredTypes(name, ClassIndex.NameKind.SIMPLE_NAME, EnumSet.of(ClassIndex.SearchScope.SOURCE, ClassIndex.SearchScope.DEPENDENCIES));
        
        for (ElementHandle<TypeElement> handle : result) {
            fqNames.add(handle.getQualifiedName());
        }
        
        return fqNames;
    }
    
    public static Project getProject(Element element){
        Project[] projects = OpenProjects.getDefault().getOpenProjects();
        
        if (projects.length == 1){
            return projects[0];
        }
        
        for (Project project : projects){
            if (!KotlinProjectHelper.INSTANCE.checkProject(project)){
                continue;
            }
            
            ClasspathInfo cpInfo = CLASSPATH_INFO.get(project);
            if (cpInfo == null) {
                continue;
            }
            
            FileObject file = SourceUtils.getFile(ElementHandle.create(element), cpInfo);

            if (file != null){
                return project;
            }
            
        }
        return null;
    }
    
    public static boolean isDeprecated(final Element element){
        Project kotlinProject = NetBeansJavaProjectElementUtils.getProject(element);
        
        if (kotlinProject == null){
            return false;
        }
        
        checkJavaSource(kotlinProject);
        IsDeprecatedSearcher searcher = new IsDeprecatedSearcher(element);
        try {
            JAVA_SOURCE.get(kotlinProject).runUserActionTask(searcher, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        
        return searcher.isDeprecated();
    }
    
    public static String toBinaryName(Project kotlinProject, final String name){
        checkJavaSource(kotlinProject);
        
        BinaryNameSearcher searcher = new BinaryNameSearcher(name);
        try {
            JAVA_SOURCE.get(kotlinProject).runUserActionTask(searcher, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return searcher.getBinaryName();
    }
    
    public static FileObject getFileObjectForElement(Element element, Project kotlinProject){
        if (element == null){
            return null;
        }
        ElementHandle<? extends Element> handle = ElementHandle.create(element);
        return SourceUtils.getFile(handle, CLASSPATH_INFO.get(kotlinProject));
    }
    
    public static void openElementInEditor(ElementHandle handle, Project kotlinProject){
//        ElementHandle<? extends Element> handle = ElementHandle.create(element);
        ElementOpen.open(CLASSPATH_INFO.get(kotlinProject), handle);
    }
    
}
