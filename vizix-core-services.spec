%define TAG %(echo $TAG)
%define BRANCH %(echo $BRANCH)
%define NAME vizix-core-corebridge

Name:   vizix-services
Version:        %{BRANCH}
Release:        %{TAG}
Summary:        vizix-core-services
Group:          vizix-core-services
License:        Propietary
#BuildRequires:
#Requires:       httpd
Vendor:         Mojix, Inc.
URL:            http://www.mojix.com/
Packager:       Mojix DevOps Dept.
Source:         vizix-core-services

%description
ViZix Services.

# %pre
# %setup
# % build
# % configure
%install
pwd
./install-war.sh %{buildroot}

%files
/usr/local/riot/services/
/usr/local/riot/services/riot-core-services/riot-core-services.war
#/etc/tomcat/Catalina/localhost/linux-riot-core-services.xml
/etc/tomcat/Catalina/localhost/riot-core-services.xml
/etc/init.d/logio
/etc/rc.d/init.d/logio
/etc/init.d/disable-transparent-hugepages
/etc/rc.d/init.d/disable-transparent-hugepages
%post
if [ -e "/usr/bin/log.io-server" ]; then
    chkconfig logio on
fi
chkconfig disable-transparent-hugepages on
chown -R tomcat:tomcat /usr/local/riot/services/riot-core-services/
#%clean
# % files
# % defattr(-,root,root,-)
# % doc

%changelog
