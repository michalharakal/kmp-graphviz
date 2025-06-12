# tools

MAKE=make
XCODEBUILD=xcodebuild
PRODUCTBUILD=productbuild
PKGBUILD=pkgbuild

# destination

DEST=/usr/local/graphviz

# base

GV_DIR=../..
BUILD=$(shell realpath "$(CURDIR)/$(GV_DIR)/build")
DOT=dot

UNAME_M:=$(shell uname -m)
ARCH=-arch $(UNAME_M)

#
# graphviz
#

graphviz-$(UNAME_M).pkg: Distribution.xml app.pkg dot.pkg
	@echo
	@echo PACKAGING GRAPHVIZ...
	@echo
	"$(PRODUCTBUILD)" --package-path . --distribution $< $@

app.pkg: Release/Graphviz.app
	@echo
	@echo PACKAGING APP...
	@echo
	"$(PKGBUILD)" --install-location /Applications --identifier com.att.graphviz.app --component $< $@

dot.pkg: $(BUILD)/bin/$(DOT) Scripts/postinstall
	@echo
	@echo PACKAGING DOT...
	@echo
	"$(PKGBUILD)" --root $(BUILD) --install-location $(DEST) --scripts Scripts --identifier com.att.graphviz $@

Release/Graphviz.app: ../*.m ../*.h $(BUILD)/bin/$(DOT)
	@echo
	@echo BUILDING GRAPHVIZ GUI...
	@echo
	"$(XCODEBUILD)" -project ../graphviz.xcodeproj -configuration Release VALID_ARCHS=$(UNAME_M) ALWAYS_SEARCH_USER_PATHS=NO MACOSX_DEPLOYMENT_TARGET=10.13 LD_RUNPATH_SEARCH_PATHS=$(DEST)/lib
	cd $@/Contents/MacOS; dyld_info -linked_dylibs Graphviz | sed -n -E "s|($(BUILD)/lib)(.*)|\1\2 @rpath\2 Graphviz|p" | xargs -t -L1 install_name_tool -change
	codesign -s "-" -fv $@/Contents/MacOS/Graphviz
	otool -L $@/Contents/MacOS/Graphviz

Scripts/postinstall:
	@echo
	@echo SCRIPTING POSTINSTALL...
	@echo
	mkdir -p $(@D)
	echo "#!/bin/sh" >$@
	echo "logger -is -t \"Graphviz Install\" \"register dot plugins\"" >>$@
	echo "$(DEST)/bin/dot -c" >>$@
	if [[ $(DEST) != /usr/local ]]; then\
 echo 'echo "$(DEST)/bin" >/etc/paths.d/graphviz' >>$@ ; fi
	chmod 755 $@

$(BUILD)/bin/$(DOT): $(GV_DIR)/cmd/dot/.libs/$(DOT)
	@echo
	@echo INSTALLING GRAPHVIZ...
	@echo
	$(MAKE) -C $(GV_DIR) install-strip
	rm -rf $(BUILD)/lib/*.la
	rm -rf $(BUILD)/lib/graphviz/*.la
	rm -rf $(BUILD)/lib/graphviz/tcl/libtcldot_builtin.*
	cd $(BUILD)/bin; find . -type f -maxdepth 1 | while read a;do dyld_info -linked_dylibs $$a | sed -n -E "s|($(BUILD))(.*)|\1\2 @executable_path/..\2 $$a|p";done | xargs -t -L1 install_name_tool -change
	cd $(BUILD)/lib; find . -type f -maxdepth 1 | while read a;do dyld_info -linked_dylibs $$a | sed -n -E "s|($(BUILD)/lib)(.*)|\1\2 @loader_path\2 $$a|p";done | xargs -t -L1 install_name_tool -change
	cd $(BUILD)/lib/graphviz; find . -type f -maxdepth 1 | while read a;do dyld_info -linked_dylibs $$a | sed -n -E "s|($(BUILD)/lib)(.*)|\1\2 @loader_path/..\2 $$a|p";done | xargs -t -L1 install_name_tool -change

$(GV_DIR)/cmd/dot/.libs/$(DOT): $(GV_DIR)/Makefile
	@echo
	@echo MAKING GRAPHVIZ...
	@echo
	if [[ -e $@ ]]; then $(MAKE) -C $(GV_DIR) clean; fi
	$(MAKE) -C $(GV_DIR)/tclpkg/tclstubs
	$(MAKE) -C $(GV_DIR)
	
$(GV_DIR)/Makefile: $(GV_DIR)/configure
	@echo
	@echo CONFIGURING GRAPHVIZ...
	@echo
	cd $(GV_DIR) && ./configure --prefix=$(BUILD) --enable-swig=no --disable-dependency-tracking --with-ipsepcola --with-quartz --with-smyrna=no --disable-perl CFLAGS="-O2 $(ARCH)" CXXFLAGS="-O2 $(ARCH)" OBJCFLAGS="-O2 $(ARCH)" LDFLAGS="$(ARCH) -Wl,-dead_strip"

.PHONY: clean
clean:
	$(MAKE) -C $(GV_DIR) clean

.PHONY: distclean
distclean:
	$(MAKE) -C $(GV_DIR) distclean