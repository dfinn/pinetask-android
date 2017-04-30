inkscape='/Applications/Inkscape.app/Contents/Resources/bin/inkscape'
dest=$PWD'/../app/src/main/res'
echo Inkscape path: $inkscape
echo Dest path: $dest
width=192
height=192

do_conversion() {
  echo Converting $1
  $inkscape -z -e $dest/drawable-ldpi/$1.png -w 36 -h 36 $PWD/$1.svg
  $inkscape -z -e $dest/drawable-mdpi/$1.png -w 48 -h 48 $PWD/$1.svg
  $inkscape -z -e $dest/drawable-hdpi/$1.png -w 72 -h 72 $PWD/$1.svg
  $inkscape -z -e $dest/drawable-ldpi/$1.png -w 96 -h 96 $PWD/$1.svg
}

do_conversion launcher_icon

