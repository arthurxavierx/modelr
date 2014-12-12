NAME = modelr
# Arthur
DEVICE = 0425793984
# Erickson
#DEVICE = 024783858a535424

all: debug reinstall

debug:
	ant debug

emulator:
	emulator -avd xavier

install:
	adb -s $(DEVICE) install bin/$(NAME)-debug.apk
reinstall:
	adb -s $(DEVICE) install -r bin/$(NAME)-debug.apk

install-emulator:
	adb -s emulator-5554 install bin/$(NAME)-debug.apk
reinstall-emulator:
	adb -s emulator-5554 install -r bin/$(NAME)-debug.apk