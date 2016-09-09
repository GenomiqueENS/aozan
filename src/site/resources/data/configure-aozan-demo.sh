#!/bin/bash

#
# Aozan demo script
#

AOZAN_VERSION=@@@VERSION@@@
AOZAN_WEBITE="http://www.outils.genomique.biologie.ens.fr/aozan"
REQUIRED_DISK_SPACE=50
AOZAN_DATA_EXAMPLE_URL="http://outils.genomique.biologie.ens.fr/leburon/downloads/aozan-example"
EXAMPLE_RUN_ID=160617_NB500892_0097_AH7N2TAFXX

ORI_DIR=$(pwd)
AOZAN_DIR="$ORI_DIR/aozan-demo"


function yes_or_no {

    if [ "$1" -eq 1 ]; then
        echo "Yes"
    else
        echo "No"
    fi
}

#
# Check the Aozan requirements
#

# Check if OS is Linux
if [ `uname` != 'Linux' ]; then
  echo >&2 "ERROR: This script can only run with Linux.  Aborting." ; exit 1
fi

# Check if wget is installed
command -v wget >/dev/null 2>&1 || { echo >&2 "ERROR: This script requires wget but it is not installed.  Aborting."; exit 1; }

# Check Java 7 is installed
command -v java >/dev/null 2>&1 || { echo >&2 "ERROR: Aozan requires Java 7 or latter but it is not installed.  Aborting."; exit 1; }

# Check bcl2fastq 2 is installed
command -v bcl2fastq >/dev/null 2>&1
if [ $? -eq 0 ]; then
    BCL2FASTQ2_INSTALLED=1
    BCL2FASTQ2_PATH=$(command -v bcl2fastq)
    BCL2FASTQ2_DIR=$(dirname "$BCL2FASTQ2_PATH")
else
    BCL2FASTQ2_INSTALLED=0
    BCL2FASTQ2_DIR=
fi

# Check blast is installed
command -v blastn >/dev/null 2>&1
if [ $? -eq 0 ]; then
    BLAST_INSTALLED=1
    BLAST_PATH=$(command -v blastn)
else
    BLAST_INSTALLED=0
    BLAST_PATH=
fi

if [ $BLAST_INSTALLED -eq 0 ]; then
    command -v blast2 >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        BLAST_INSTALLED=1
        BLAST_PATH=$(command -v blastall)
    else
        BLAST_INSTALLED=0
        BLAST_PATH=
    fi
fi

# Check Docker is installed
command -v docker >/dev/null 2>&1
if [ $? -eq 0 ]; then
    DOCKER_INSTALLED=1
else
    DOCKER_INSTALLED=0
fi


# Check if Bcl2fastq can be launched
if [ "$BCL2FASTQ2_INSTALLED" -eq 0 ] && [ "$DOCKER_INSTALLED" -eq 0 ]; then
    echo >&2 "ERROR: The Aozan demo requires bcl2fastq.  Aborting."; exit 1;
fi

# Check space disk
FREE_SPACE=$(df -BG . | tail -n 1 | tr -s ' ' | cut -f 4 -d ' ' | tr -d 'G')
if [ "$FREE_SPACE" -lt "$REQUIRED_DISK_SPACE" ]; then
    echo >&2 "ERROR: The Aozan demo requires $REQUIRED_DISK_SPACE GB.  Aborting."; exit 1;
fi

#
# User configuration
#

echo -e "Aozan demo installer\n"

# Ask for Email configuration
read -r -p "Enable email? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        EMAIL_ENABLED=1
        ;;
    *)
        EMAIL_ENABLED=0
        ;;
esac

if [ $EMAIL_ENABLED -eq 1 ]; then

    read -r -p "SMTP server: " SMTP_SERVER_NAME
    read -r -p "User email: " USER_EMAIL
fi

# Ask to enable Blast
if [ $BLAST_INSTALLED -eq 1 ]; then
    read -r -p "Enable Blast for overrepresenting sequences in FastQC? [y/N] " response
    case $response in
        [yY][eE][sS]|[yY])
            BLAST_ENABLED=1
            ;;
        *)
            BLAST_ENABLED=0
            ;;
    esac
else
    BLAST_ENABLED=0
fi

# Ask to enable FastQ Screen
read -r -p "Enable FastQ Screen?  [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        FASTQ_SCREEN_ENABLED=1
        ;;
    *)
        FASTQ_SCREEN_ENABLED=0
        ;;
esac


#
# Summary
#

echo -e "\nSystem configuration:"
echo -e "  - Docker installed\t\t:" $(yes_or_no $DOCKER_INSTALLED)
echo -e "  - Bcl2fastq2 installed\t:" $(yes_or_no $BCL2FASTQ2_INSTALLED)
if [ $BCL2FASTQ2_INSTALLED -eq 1 ]; then
    echo -e "  - Bcl2fastq2 path\t\t:" $BCL2FASTQ2_PATH
fi

echo -e "  - Blast installed\t\t:" $(yes_or_no $BLAST_INSTALLED)
if [ $BLAST_INSTALLED -eq 1 ]; then
    echo -e "  - Blast path\t\t\t:" $BLAST_PATH
fi

echo -e "\nDownloads:"
echo -e "  - Aozan binary (163 MB)\t: Yes"
echo -e "  - Aozan demo run (3.5 GB)\t:" $(yes_or_no $BLAST_ENABLED)
echo -e "  - FastQ Screen genomes\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "     - Mouse (698.8 MB)\t\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "     - Adapters (11.2 KB)\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "     - PhiX (1.7 KB)\t\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "     - lsuref (10.4 MB)\t\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "     - ssuref (100.3 MB)\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "  - NCBI NT database (26.3 GB)\t:" $(yes_or_no $EMAIL_ENABLED)

echo -e "\nAozan demo configuration:"
echo -e "  - Blast enabled\t\t:" $(yes_or_no $BLAST_ENABLED)
echo -e "  - FastQ Screen enabled\t:" $(yes_or_no $FASTQ_SCREEN_ENABLED)
echo -e "  - Email enabled\t\t:" $(yes_or_no $EMAIL_ENABLED)

if [ $EMAIL_ENABLED -eq 1 ]; then
    echo -e "  - SMTP server\t\t\t:" $SMTP_SERVER_NAME
    echo -e "  - User email\t\t\t:" $USER_EMAIL
fi

# Ask to validate connfiguration
echo
read -r -p "Is configuration correct? [y/N] " response
case $response in
    [yY][eE][sS]|[yY])
        ;;
    *)
        echo >&2 "Abort."; exit 1;
        ;;
esac

echo

#
# Create Aozan directories
#

if [ -d "$AOZAN_DIR" ]; then
    echo >&2 "ERROR: The Aozan demo directory already exists: "$AOZAN_DIR"  Aborting."; exit 1;
fi

echo "* Create Aozan demo directories"

mkdir "$AOZAN_DIR"
if [ $? -ne 0 ]; then
    echo >&2 "ERROR: Unable to create the Aozan demo directory: "$AOZAN_DIR"  Aborting."; exit 1;
fi

mkdir "$AOZAN_DIR"/resources
mkdir "$AOZAN_DIR"/resources/genomes
mkdir "$AOZAN_DIR"/resources/genome-descriptions
mkdir "$AOZAN_DIR"/resources/mapper-indexes
mkdir "$AOZAN_DIR"/resources/ncbi-database-nt

mkdir "$AOZAN_DIR"/samplesheets
mkdir "$AOZAN_DIR"/var
mkdir "$AOZAN_DIR"/tmp
mkdir "$AOZAN_DIR"/conf

mkdir "$AOZAN_DIR"/sequencer-output
mkdir "$AOZAN_DIR"/bcl
mkdir "$AOZAN_DIR"/fastq
mkdir "$AOZAN_DIR"/runs


echo "* Download example configuration file"
cd "$AOZAN_DIR"/conf
wget -q "$AOZAN_WEBITE/aozan-example.conf"
mv aozan-example.conf aozan.conf
cd "$AOZAN_DIR"

echo "* Update configuration file"

# Email configuration
if [ "$EMAIL_ENABLED" -eq 0 ]; then
    sed -i "s/send.mail=True/send.mail=False/" "$AOZAN_DIR"/conf/aozan.conf
else
    sed -i "s/smtp.server=smtp.example.com/smtp.server=$SMTP_SERVER_NAME/" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s/mail.from=aozan@example.com/mail.from=$USER_EMAIL/" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s/mail.to=me@example.com/mail.to=$USER_EMAIL/" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s/mail.error.to=aozan-errors@example.com/mail.error.to=$USER_EMAIL/" "$AOZAN_DIR"/conf/aozan.conf
fi

# Aozan configuration files directory
sed -i "s#aozan.var.path=/path/to/aozan/data#aozan.var.path=$AOZAN_DIR/var#" "$AOZAN_DIR"/conf/aozan.conf

# Aozan log file
sed -i "s#aozan.log.path=/var/log/aozan.log#aozan.log.path=$AOZAN_DIR/aozan.log#" "$AOZAN_DIR"/conf/aozan.conf

# Paths where HiSeq write data, two path separated by ':'
sed -i "s#hiseq.data.path=/path/to/hiseq/output#hiseq.data.path=$AOZAN_DIR/sequencer-output#" "$AOZAN_DIR"/conf/aozan.conf

# Where store bcl files
sed -i "s#bcl.data.path=/path/to/bcl#bcl.data.path=$AOZAN_DIR/bcl#" "$AOZAN_DIR"/conf/aozan.conf

# Where store fastq files
sed -i "s#fastq.data.path=/path/to/fastq#fastq.data.path=$AOZAN_DIR/fastq#" "$AOZAN_DIR"/conf/aozan.conf

# Were store runs informations and qc data
sed -i "s#reports.data.path=/path/to/runs#reports.data.path=$AOZAN_DIR/runs#" "$AOZAN_DIR"/conf/aozan.conf

# Where store Bcl2fastq samplesheet files in XLS or CSV format
sed -i "s#bcl2fastq.samplesheet.path=/path/to/samplesheets#bcl2fastq.samplesheet.path=$AOZAN_DIR/samplesheets#" "$AOZAN_DIR"/conf/aozan.conf

# File that contains alias for barcodes
sed -i "s#index.sequences=/path/to/aozan/resources/indexes-sequences.txt#index.sequences=$AOZAN_DIR/conf/index-sequences.aliases#" "$AOZAN_DIR"/conf/aozan.conf

# Temporary directory
sed -i "s#tmp.path=/tmp#tmp.path=$AOZAN_DIR/tmp#" "$AOZAN_DIR"/conf/aozan.conf

# Lock file path
sed -i "s#aozan.lock.file=/var/lock/aozan.lock#aozan.lock.file=$AOZAN_DIR/var/aozan.lock#" "$AOZAN_DIR"/conf/aozan.conf



# Path to a specific contaminants list, replace list per default
sed -i "s#qc.conf.fastqc.contaminant.file=/path/to/aozan/resources/contaminants_fastqc.txt#qc.conf.fastqc.contaminant.file=$AOZAN_DIR/conf/resources/contaminants_fastqc.txt##" "$AOZAN_DIR"/conf/aozan.conf

# Path to a specific adapter file, replace default file
sed -i "s#qc.conf.fastqc.adapter.file=/path/to/aozan/resources/adapters.txt#qc.conf.fastqc.adapter.file=$AOZAN_DIR/conf/adapters.txt#" "$AOZAN_DIR"/conf/aozan.conf

# Path to a specific limits file, replace default file
sed -i "s#qc.conf.fastqc.limits.file=/path/to/aozan/resources/limits_modules.txt#qc.conf.fastqc.limits.file=$AOZAN_DIR/conf/limits_modules.txt#" "$AOZAN_DIR"/conf/aozan.conf



# Path to the file which make the correspondence between genome name in bcl2fastq samplesheet and the reference genome name
sed -i "s#qc.conf.fastqscreen.genome.aliases.path=/path/to/aozan/resources/alias_name_genome_fastqscreen.txt#qc.conf.fastqscreen.genome.aliases.path=$AOZAN_DIR/conf/genome-name-aliases.txt#" "$AOZAN_DIR"/conf/aozan.conf

# Path to the genomes descriptions repository
sed -i "s#qc.conf.fastqscreen.genome.descs.path=/path/to/aozan/resources/genomes_descs#qc.conf.fastqscreen.genome.descs.path=$AOZAN_DIR/resources/genome-descriptions#" "$AOZAN_DIR"/conf/aozan.conf

# Path to the genomes repository
sed -i "s#qc.conf.fastqscreen.genomes.path=/path/to/aozan/resources/genomes#qc.conf.fastqscreen.genomes.path=$AOZAN_DIR/resources/genomes#" "$AOZAN_DIR"/conf/aozan.conf

# Path to the genomes indexes repository
sed -i "s#qc.conf.fastqscreen.mapper.indexes.path=/path/to/aozan/resources/mappers_indexes#qc.conf.fastqscreen.mapper.indexes.path=$AOZAN_DIR/resources/mapper-indexes#" "$AOZAN_DIR"/conf/aozan.conf



# Path to the database nt, access to nt.pal file
sed -i "s#qc.conf.fastqc.blast.db.path=/path/to/ncbi_database_nt#qc.conf.fastqc.blast.db.path=$AOZAN_DIR/resources/ncbi-database-nt#" "$AOZAN_DIR"/conf/aozan.conf


# Set Bcl2fastq2 path
if [ "$BCL2FASTQ2_DIR" != "" ]; then
    sed -i "s#bcl2fastq.path=/usr/local/bcl2fastq#bcl2fastq.path=$BCL2FASTQ2_DIR#" "$AOZAN_DIR"/conf/aozan.conf
fi

# Enable Blast
if [ "$EMAIL_ENABLED" -eq 1 ]; then
    sed -i "s#qc.conf.fastqc.blast.enable=False#qc.conf.fastqc.blast.enable=True#" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s#qc.conf.fastqc.blast.path=/usr/bin/blastall#qc.conf.fastqc.blast.path=$BLAST_PATH#" "$AOZAN_DIR"/conf/aozan.conf
fi


# Disable FastQ Screen
if [ "$EMAIL_ENABLED" -eq 1 ]; then
    sed -i "s#qc.test.sample.fastqscreen.mapped.except.ref.genome.percent.enable=True#qc.test.sample.fastqscreen.mapped.except.ref.genome.percent.enable=False#" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s#qc.test.sample.fastqscreen.mapped.percent.enable=True#qc.test.sample.fastqscreen.mapped.percent.enable=False#" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s#qc.test.sample.fastqscreen.report.enable=True#qc.test.sample.fastqscreen.report.enable=False#" "$AOZAN_DIR"/conf/aozan.conf
fi

if [ "$DOCKER_INSTALLED" -eq 1 ]; then
    sed -i "s#demux.use.docker.enable=False#demux.use.docker.enable=True#" "$AOZAN_DIR"/conf/aozan.conf
    sed -i "s#qc.conf.fastqc.blast.use.docker=False#qc.conf.fastqc.blast.use.docker=True#" "$AOZAN_DIR"/conf/aozan.conf
fi

# Ask to validate connfiguration
echo
read -r -p "Download and install Aozan and all its dependencies and data example? [Y/n] " response
case $response in
    [nN][oO]|[nN])
        echo >&2 "Abort."; exit 1;
        ;;
    *)
        ;;
esac

echo

#
# Install Aozan
#

echo "* Download Aozan"
cd "$AOZAN_DIR"
wget "$AOZAN_WEBITE/aozan-$AOZAN_VERSION.tar.gz"
echo "* Install Aozan"
tar xzf aozan-$AOZAN_VERSION.tar.gz

#
# Copy resources
#


echo "* Download Aozan raw data example run"
cd "$AOZAN_DIR"/sequencer-output
wget "$AOZAN_DATA_EXAMPLE_URL/$EXAMPLE_RUN_ID.tar.bz2"

echo "* Unpack Aozan raw data example run"
tar xjf "$EXAMPLE_RUN_ID.tar.bz2"

echo "* Download index sequence aliases"
cd "$AOZAN_DIR"/conf
wget "$AOZAN_WEBITE/data/index-sequences.aliases"


cd "$AOZAN_DIR"/samplesheets

echo "* Download Samplesheet example"
wget "$AOZAN_WEBITE/data/SampleSheet.xls"
mv SampleSheet.xls samplesheet_NB500892_0097.xls

echo "* Download Samplesheet template"
wget "$AOZAN_WEBITE/samplesheet_bcl2fastq2_template.xls"


if [ "$FASTQ_SCREEN_ENABLED" -eq 1 ]; then

    cd "$AOZAN_DIR"/resources/genomes

    echo "* Download Mouse genome"
    wget "$AOZAN_DATA_EXAMPLE_URL/mm9.fasta.bz2"
    mv genome.fasta.bz2 mm9.fasta.bz2

    echo "* Download PhiX genome"
    wget "$AOZAN_DATA_EXAMPLE_URL/phix.fasta.bz2"

    echo "* Download adapters sequences"
    wget "$AOZAN_DATA_EXAMPLE_URL/adapters.fasta"

    echo "* Download lsuref sequences"
    wget "$AOZAN_DATA_EXAMPLE_URL/LSURef_111_tax_silva_trunc.fasta.bz2"
    ln -s LSURef_111_tax_silva_trunc.fasta.bz2 lsuref.fasta.bz2

    echo "* Download ssuref sequences"
    wget "$AOZAN_DATA_EXAMPLE_URL/SSURef_111_tax_silva_trunc.fasta.bz2"
    ln -s SSURef_111_tax_silva_trunc.fasta.bz2 ssuref.fasta.bz2

    echo "* Create genome alias file"
    echo "mouse=mm9" > "$AOZAN_DIR"/conf/genome-name-aliases.txt
fi

if [ "$BLAST_ENABLED" -eq 1 ]; then

    echo "* Download NCBI NT database"
    cd "$AOZAN_DIR/resources/ncbi-database-nt"
    wget ftp://ftp.ncbi.nlm.nih.gov/blast/db/nt.??.tar.gz*

    echo "* Check downloaded NCBI NT database"
    md5sum -c *.md5
    if [ $? -ne 0 ]; then
        echo >&2 "Fail to check downloaded NCBI NT database.  Aborting."; exit 1;
    fi

    echo "* Unpack NCBI NT database"
    for f in nt.??.tar.gz ; do tar xzf $f ; done
fi



#
# Final message
#

echo
echo "All the files required by the Aozan demo has been downloaded and a configuration file has been created."
echo "You can now launch the Aozan demo using the following command:"
echo "\$ $AOZAN_DIR/aozan-$AOZAN_VERSION/aozan.sh $AOZAN_DIR/conf/aozan.conf"
echo
echo "Warning: The first QC may be very long if FastQ Screen is enabled because Bowtie indexes for genomes (e.g. Mouse) must be computed."


