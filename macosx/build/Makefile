# tools

MAKE=make
XCODEBUILD=xcodebuild
PRODUCTBUILD=productbuild
PKGBUILD=pkgbuild

# prefix

PREFIX=/usr/local
FIXDIR=~/FIX/macOS

# base

GV_DIR=../..
DOT=dot

UNAME_M=$(shell uname -m)
ARCH=-arch $(UNAME_M)

#
# graphviz
#

graphviz-$(UNAME_M).pkg: Distribution.xml app.pkg dot.pkg
	@echo
	@echo PACKAGING GRAPHVIZ...
	@echo
	"$(PRODUCTBUILD)" --package-path . --distribution $< $@

app.pkg: Component.plist Release/Graphviz.app
	@echo
	@echo PACKAGING APP...
	@echo
	rm -rf Release/Graphviz.app.dSYM
	"$(PKGBUILD)" --root Release --install-location /Applications --identifier com.att.graphviz.app --component-plist $< $@

dot.pkg: .$(PREFIX)/bin/$(DOT) Scripts/postinstall
	@echo
	@echo PACKAGING DOT...
	@echo
	"$(PKGBUILD)" --root .$(PREFIX) --install-location $(PREFIX) --scripts Scripts --identifier com.att.graphviz $@

.$(PREFIX)/bin/$(DOT): $(GV_DIR)/cmd/dot/.libs/$(DOT)
	@echo
	@echo INSTALLING GRAPHVIZ...
	@echo
	$(MAKE) DESTDIR=$(CURDIR) -C $(GV_DIR) install-strip
	rm -rf .$(PREFIX)/lib/*.la
	rm -rf .$(PREFIX)/lib/graphviz/*.la
	rm -rf .$(PREFIX)/lib/graphviz/tcl/libtcldot_builtin.*
#	cp $(FIXDIR)/bin/fc-cache .$(PREFIX)/bin/gv-fc-cache
	
Release/Graphviz.app: ../*.m ../*.h $(GV_DIR)/cmd/dot/.libs/$(DOT)
	@echo
	@echo BUILDING GRAPHVIZ GUI...
	@echo
	"$(XCODEBUILD)" -project ../graphviz.xcodeproj -configuration Release VALID_ARCHS=$(UNAME_M) ALWAYS_SEARCH_USER_PATHS=NO MACOSX_DEPLOYMENT_TARGET=10.13

Scripts/postinstall:
	@echo
	@echo SCRIPTING POSTINSTALL...
	@echo
	mkdir -p $(@D)
	echo "#!/bin/sh" >$@
	echo "logger -is -t \"Graphviz Install\" \"register dot plugins\"" >>$@
	echo "$(PREFIX)/bin/dot -c" >>$@
	if [[ $(PREFIX) != /usr/local ]]; then\
 echo 'echo "$(PREFIX)/bin" >/etc/paths.d/graphviz' >>$@ ; fi
	chmod 755 $@

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
	cd $(GV_DIR) && ./configure --prefix=$(PREFIX) --enable-swig=no --disable-dependency-tracking --with-ipsepcola --with-quartz --with-smyrna=no --disable-perl --with-extra-includedir="$(FIXDIR)/include" --with-extra-libdir="$(FIXDIR)/lib" CFLAGS="-O2 $(ARCH)" CXXFLAGS="-O2 $(ARCH)" OBJCFLAGS="-O2 $(ARCH)" LDFLAGS="$(ARCH) -Wl,-dead_strip" GDLIB_CONFIG="$(FIXDIR)/bin/gdlib-config" PKG_CONFIG="$(FIXDIR)/bin/pkg-config" PKG_CONFIG_PATH="$(FIXDIR)/lib/pkgconfig:/usr/X11/lib/pkgconfig"

.PHONY: clean
clean:
	$(MAKE) -C $(GV_DIR) clean

.PHONY: distclean
distclean:
	$(MAKE) -C $(GV_DIR) distclean
