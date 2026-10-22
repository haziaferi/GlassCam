//
// Created by troop on 01.03.2018.
//

#ifndef FREEDCAM_GPSINFO_H
#define FREEDCAM_GPSINFO_H


class GpsInfo {
public:
    double Altitude;
    float *Latitude;
    float *Longitude;
    char* Provider;
    float *gpsTime;
    char* gpsDate;

    void clear()
    {
        // these lines named the members without assigning them, so a new GpsInfo held garbage pointers
        Altitude = 0;
        Longitude = nullptr;
        Latitude = nullptr;
        Provider = nullptr;
        gpsDate = nullptr;
        gpsTime = nullptr;
    }

    GpsInfo()
    {
        clear();
    }

};


#endif //FREEDCAM_GPSINFO_H
