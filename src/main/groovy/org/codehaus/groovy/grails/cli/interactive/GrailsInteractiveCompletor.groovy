/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.cli.interactive

import grails.build.interactive.completors.StringsCompleter
import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import grails.build.interactive.completors.EscapingFileNameCompletor
import grails.build.interactive.completors.RegexCompletor
import jline.console.completer.ArgumentCompleter
import jline.console.completer.Completer

import java.util.concurrent.ConcurrentHashMap


import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import groovy.transform.CompileStatic

/**
 * A JLine completer for Grails' interactive mode.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@CompileStatic
class GrailsInteractiveCompletor extends StringsCompleter {
    BuildSettings settings
    Map completorCache = new ConcurrentHashMap()

    private ArgumentCompleter bangCompletor = new ArgumentCompleter(
        new RegexCompletor("!\\w+"), new EscapingFileNameCompletor())

    GrailsInteractiveCompletor(BuildSettings settings, List<File> scriptResources) throws IOException{
        super((String[]) getScriptNames(scriptResources))
        this.settings = settings
    }

    @Override
    int complete(String buffer, int cursor, List clist) {
        final String trimmedBuffer = buffer.trim()
        if (!trimmedBuffer) {
            return super.complete(buffer, cursor, clist)
        }

        if (trimmedBuffer.contains(' ')) {
            trimmedBuffer = trimmedBuffer.split(' ')[0]
        }

        Completer completer = (Completer) (trimmedBuffer[0] == '!' ? bangCompletor : completorCache.get(trimmedBuffer))
        if (completer == null) {
            def className = GrailsNameUtils.getNameFromScript(trimmedBuffer)
            className = "grails.build.interactive.completors.$className"

            try {
                def completorClass = getClass().classLoader.loadClass(className)
                completer = (Completer)completorClass.newInstance()
                if (completer instanceof BuildSettingsAware) {
                    completer.buildSettings = settings
                }
                completorCache.put(trimmedBuffer, completer)
            } catch (e) {
                return super.complete(buffer, cursor, clist)
            }
        }

        try {
            return completer.complete(buffer, cursor, clist)
        } catch (e) {
            return super.complete(buffer, cursor, clist)
        }
    }

    static String[] getScriptNames(List<File> scriptResources) {
        final scriptNames = scriptResources.collect {  File f -> GrailsNameUtils.getScriptName(f.name) }
        scriptNames.remove('create-app')
        scriptNames.remove('install-plugin')
        scriptNames.remove('uninstall-plugin')
        scriptNames << "open"
        scriptNames << "exit"
        scriptNames << "restart-daemon"
        scriptNames << "start-daemon"
        scriptNames << "quit"
        scriptNames << "stop-app"
        scriptNames as String[]
    }
}
